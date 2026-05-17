param(
    [string]$UserSvc = $(if ($env:USER_SERVICE_URL) { $env:USER_SERVICE_URL } else { "http://localhost:8081" }),
    [string]$RestaurantSvc = $(if ($env:RESTAURANT_SERVICE_URL) { $env:RESTAURANT_SERVICE_URL } else { "http://localhost:8082" }),
    [string]$OrderSvc = $(if ($env:ORDER_SERVICE_URL) { $env:ORDER_SERVICE_URL } else { "http://localhost:8083" }),
    [string]$DeliverySvc = $(if ($env:DELIVERY_SERVICE_URL) { $env:DELIVERY_SERVICE_URL } else { "http://localhost:8084" }),
    [string]$CheckoutSvc = $(if ($env:CHECKOUT_SERVICE_URL) { $env:CHECKOUT_SERVICE_URL } else { "http://localhost:8085" }),
    [string]$RabbitHost = $(if ($env:RABBIT_HOST) { $env:RABBIT_HOST } else { "http://localhost:15672" }),
    [string]$RabbitUser = $(if ($env:RABBIT_USER) { $env:RABBIT_USER } else { "guest" }),
    [string]$RabbitPass = $(if ($env:RABBIT_PASS) { $env:RABBIT_PASS } else { "guest" }),
    [string[]]$RabbitQueues = @(
        "user.order.saga-listener",
        "delivery.saga-listener",
        "payment.saga-listener",
        "order.saga-feedback"
    ),
    [int]$PollIntervalSeconds = 2,
    [int]$PollTimeoutSeconds = 120
)

function NowTs { (Get-Date).ToString("yyyyMMdd-HHmmssfff") }
$LogDir = Join-Path (Get-Location) "logs"
if (-not (Test-Path $LogDir)) { New-Item -Path $LogDir -ItemType Directory | Out-Null }

function Write-TextLog($prefix, $message) {
    $fileName = "{0}.log" -f $prefix
    $fn = Join-Path $LogDir $fileName
    $message | Out-File -FilePath $fn -Encoding utf8
    Write-Host ("Wrote log: {0}" -f $fn)
    return $fn
}

function Get-ShortSummary($value) {
    if ($null -eq $value) { return "<null>" }
    if ($value -is [string]) { return $value }
    if ($value.PSObject.Properties.Name -contains "status") {
        return "status=$($value.status)"
    }
    if ($value.PSObject.Properties.Name -contains "id") {
        return "id=$($value.id)"
    }
    if ($value.PSObject.Properties.Name -contains "message") {
        return $value.message
    }
    return $value.ToString()
}

function Get-ErrorSummary($errorRecord) {
    $exception = $errorRecord.Exception
    $stack = @()
    if ($exception.StackTrace) {
        $stack += $exception.StackTrace.Trim()
    }
    if ($errorRecord.ScriptStackTrace) {
        $stack += $errorRecord.ScriptStackTrace.Trim()
    }
    $stackText = if ($stack.Count -gt 0) { $stack -join "`n" } else { "<no stack trace>" }
    return @(
        "message: $($exception.Message)",
        "stack:",
        $stackText
    ) -join [Environment]::NewLine
}

function Invoke-Api($Method,$Url,$Body=$null,$Headers=@{},[switch]$AllowFailure) {
    Write-Host ("HTTP -> {0} {1}" -f $Method, $Url)
    try {
        if ($Body -ne $null) {
            $bodyJson = $Body | ConvertTo-Json -Depth 6
            $resp = Invoke-RestMethod -Method $Method -Uri $Url -Body $bodyJson -ContentType "application/json" -Headers $Headers -TimeoutSec 30 -ErrorAction Stop
        } else {
            $resp = Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -TimeoutSec 30 -ErrorAction Stop
        }
        Write-Host ("HTTP <- {0} {1}: OK" -f $Method, $Url)
        Write-TextLog ("http_" + ($Url -replace '[^\w]','_')) ("OK {0} {1} -> {2}" -f $Method, $Url, (Get-ShortSummary $resp)) | Out-Null
        return $resp
    } catch {
        Write-Host ("HTTP ERR {0} {1}: {2}" -f $Method, $Url, $_.Exception.Message)
        Write-TextLog ("http_error_" + ($Url -replace '[^\w]','_')) (("ERR {0} {1}`n{2}" -f $Method, $Url, (Get-ErrorSummary $_))) | Out-Null
        if ($AllowFailure) { return $null } else { throw $_ }
    }
}

function Extract-Token($resp) {
    if (-not $resp) { return $null }
    return $resp.token ?? $resp.accessToken ?? $resp.jwt ?? $resp.data?.token
}

function Convert-Base64UrlToBase64([string]$value) {
    $value = $value.Replace('-', '+').Replace('_', '/')
    switch ($value.Length % 4) {
        2 { $value += '==' }
        3 { $value += '=' }
    }
    return $value
}

function Decode-JwtPayload([string]$token) {
    if (-not $token) { return $null }
    $parts = $token.Split('.')
    if ($parts.Length -lt 2) { return $null }
    $payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((Convert-Base64UrlToBase64 $parts[1])))
    return $payload | ConvertFrom-Json
}

function Resolve-UserIdByEmail($email, $headers) {
    $users = Invoke-Api -Method GET -Url "$UserSvc/api/users/search?email=$([System.Uri]::EscapeDataString($email))" -Headers $headers -AllowFailure
    if (-not $users) { return $null }
    if ($users -is [System.Array]) {
        $match = $users | Where-Object { $_.email -eq $email } | Select-Object -First 1
        if ($match) { return [int]$match.id }
        if ($users.Count -gt 0 -and $users[0].id) { return [int]$users[0].id }
    } elseif ($users.id) {
        return [int]$users.id
    }
    return $null
}

function Resolve-OpenRestaurantId($headers) {
    $restaurants = Invoke-Api -Method GET -Url "$RestaurantSvc/api/restaurants" -Headers $headers -AllowFailure
    if ($restaurants -is [System.Array]) {
        $open = $restaurants | Where-Object { ($_.status -as [string]) -eq "OPEN" } | Select-Object -First 1
        if ($open) { return [int]$open.id }
        if ($restaurants.Count -gt 0 -and $restaurants[0].id) { return [int]$restaurants[0].id }
    }
    return $null
}

function Ensure-RestaurantId($headers) {
    $restaurantId = Resolve-OpenRestaurantId -headers $headers
    if ($restaurantId) { return $restaurantId }

    Write-Host "No restaurant found; creating a fallback restaurant..."
    $restaurantEmail = "resto+$(NowTs)@example.com"
    $restaurantPhone = "07$((NowTs) -replace '[^0-9]','')"
    $created = Invoke-Api -Method POST -Url "$RestaurantSvc/api/restaurants" -Body @{ name="Test Resto $(NowTs)"; email=$restaurantEmail; phone=$restaurantPhone; cuisineType="FAST_FOOD"; details=@{ description = "Automation test restaurant" } } -Headers $headers -AllowFailure
    if ($created -and $created.id) { return [int]$created.id }

    $restaurants = Invoke-Api -Method GET -Url "$RestaurantSvc/api/restaurants" -Headers $headers -AllowFailure
    if ($restaurants -is [System.Array]) {
        $match = $restaurants | Where-Object { $_.email -eq $restaurantEmail } | Select-Object -First 1
        if ($match) { return [int]$match.id }
    }

    return $null
}

# 1) Seed admin (best-effort)
Write-Host "Seeding admin (best-effort)..."
try { Invoke-Api -Method GET -Url "$UserSvc/api/seed" -AllowFailure } catch { }

# 2) Login admin
Write-Host "Logging in admin..."
$adminResp = Invoke-Api -Method POST -Url "$UserSvc/api/auth/login" -Body @{ email="admin@guc.edu.eg"; password="admin123" } -AllowFailure
$ADMIN_TOKEN = Extract-Token $adminResp
$ADMIN_HDR = @{}
if ($ADMIN_TOKEN) { $ADMIN_HDR = @{ Authorization = "Bearer $ADMIN_TOKEN" } }
$ADMIN_PAYLOAD = Decode-JwtPayload $ADMIN_TOKEN
$ORDER_USER_ID = if ($ADMIN_PAYLOAD -and $ADMIN_PAYLOAD.uid) { [int]$ADMIN_PAYLOAD.uid } else { 1 }

# 3) Register test user and get token
Write-Host "Registering test user..."
$userEmail = "test.user+$(NowTs)@example.com"
$userPhone = "0100$((NowTs) -replace '[^0-9]','')"
$reg = Invoke-Api -Method POST -Url "$UserSvc/api/auth/register" -Body @{ name="Test User"; email=$userEmail; password="testpass"; phone=$userPhone } -AllowFailure
$USER_TOKEN = Extract-Token $reg
if (-not $USER_TOKEN) {
    Write-Host "Register returned no token; trying login for existing user..."
    $loginResp = Invoke-Api -Method POST -Url "$UserSvc/api/auth/login" -Body @{ email=$userEmail; password="testpass" } -AllowFailure
    $USER_TOKEN = Extract-Token $loginResp
}
$USER_HDR = @{}
if ($USER_TOKEN) { $USER_HDR = @{ Authorization = "Bearer $USER_TOKEN" } }

$jwtPayload = Decode-JwtPayload $USER_TOKEN
if ($jwtPayload -and $jwtPayload.uid) {
    $USER_ID = [int]$jwtPayload.uid
}
if (-not $USER_ID) {
    $USER_ID = Resolve-UserIdByEmail -email $userEmail -headers $ADMIN_HDR
}
if (-not $USER_ID -and $reg) {
    $USER_ID = ($reg.id ?? $reg.userId) -as [int]
}
if (-not $USER_ID) {
    throw "Could not resolve test user id for $userEmail"
}

# 4) Create restaurant (use admin)
Write-Host "Resolving restaurant..."
$RESTO_ID = Ensure-RestaurantId -headers $ADMIN_HDR
if (-not $RESTO_ID) {
    throw "Could not resolve a usable restaurant id"
}

# 5) Create order (as user)
Write-Host "Creating order..."
$orderResp = Invoke-Api -Method POST -Url "$OrderSvc/api/orders" -Body @{
    userId = $ORDER_USER_ID
    restaurantId = $RESTO_ID
    status = "PREPARING"
    totalAmount = 180.0
    deliveryAddress = "123 Test St"
    metadata = @{}
    orderDate = "2026-09-30T23:59:59.9999"
} -Headers $ADMIN_HDR -AllowFailure
$ORDER_ID = $orderResp.id
if (-not $ORDER_ID) {
    throw "Order creation did not return an id"
}

# 6) Create active delivery (use admin)
Write-Host "Creating delivery..."
$delivery = Invoke-Api -Method POST -Url "$DeliverySvc/api/deliveries/order/$ORDER_ID" -Body @{ driverName="Driver A"; latitude=30.0444; longitude=31.2357; status="IN_TRANSIT"; metadata=@{} } -Headers $ADMIN_HDR -AllowFailure
$DELIVERY_ID = $delivery.id
if (-not $DELIVERY_ID) {
    throw "Delivery creation did not return an id"
}

Write-TextLog "order_preparing" ("orderId={0} status=PREPARING" -f $ORDER_ID) | Out-Null

# helper poll
function Poll-Until($Url, $CheckBlock, $Interval=2, $Timeout=120) {
    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        $r = Invoke-Api -Method GET -Url $Url -Headers $USER_HDR -AllowFailure
        if (& $CheckBlock $r) { return $r }
        Start-Sleep -Seconds $Interval
    }
    throw "Timeout waiting for condition: $Url"
}

function Get-OrderDetailsStatus([long]$orderId) {
    $details = Invoke-Api -Method GET -Url "$OrderSvc/api/orders/$orderId/details" -Headers $USER_HDR -AllowFailure
    if (-not $details) { return $null }
    return $details.status
}

function Poll-OrderStatus([long]$orderId, [string]$expectedStatus, $Interval=2, $Timeout=120) {
    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        $status = Get-OrderDetailsStatus -orderId $orderId
        if ($status -eq $expectedStatus) { return $status }
        Start-Sleep -Seconds $Interval
    }
    throw "Timeout waiting for order $orderId to reach $expectedStatus"
}

# 7) Trigger deliver (ADMIN)
Write-Host "Triggering deliver endpoint..."
try {
    $deliverResp = Invoke-Api -Method PUT -Url "$OrderSvc/api/orders/$ORDER_ID/deliver" -Headers @{ Authorization = "Bearer $ADMIN_TOKEN"; "X-User-Role"="ADMIN" } -AllowFailure
    Write-TextLog "deliver_trigger_response" ("orderId={0} response={1}" -f $ORDER_ID, (Get-ShortSummary $deliverResp)) | Out-Null
} catch {
    Write-Host "Deliver call failed: $_"
}

# 8) Capture rabbit after deliver
function Capture-RabbitQueues {
    param([string[]]$Queues, [int]$Count=200)
    $b64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f $RabbitUser, $RabbitPass)))
    $hdr = @{ Authorization = "Basic $b64" }
    foreach ($q in $Queues) {
        $vhostEnc = [System.Web.HttpUtility]::UrlEncode("/")
        $url = "$RabbitHost/api/queues/$vhostEnc/$q/get"
        $body = @{ count = $Count; ackmode = "ack_requeue_true"; encoding = "auto"; truncate = 50000 } | ConvertTo-Json -Depth 6
        try {
            $resp = Invoke-RestMethod -Method POST -Uri $url -Headers $hdr -Body $body -ContentType "application/json" -TimeoutSec 30 -ErrorAction Stop
            Write-TextLog ("rabbit_" + ($q -replace '[^A-Za-z0-9]','_')) ("queue={0} messages={1}" -f $q, (@($resp).Count)) | Out-Null
        } catch {
            Write-TextLog ("rabbit_err_" + ($q -replace '[^A-Za-z0-9]','_')) ("queue={0} error={1}" -f $q, $_.Exception.Message) | Out-Null
            Write-Host ("Failed to fetch queue {0}: {1}" -f $q, $_.Exception.Message)
        }
    }
}

Write-Host "Capturing RabbitMQ messages (after deliver)..."
Capture-RabbitQueues -Queues $RabbitQueues -Count 200

# 9) Poll until PAYMENT_PENDING (order)
Write-Host "Polling order until PAYMENT_PENDING..."
try {
    $orderStatus = Poll-OrderStatus -orderId $ORDER_ID -expectedStatus "PAYMENT_PENDING" -Interval $PollIntervalSeconds -Timeout $PollTimeoutSeconds
    Write-TextLog "order_payment_pending" ("orderId={0} status={1}" -f $ORDER_ID, $orderStatus) | Out-Null
} catch {
    Write-Host "Timeout waiting for PAYMENT_PENDING: $_"
}

# 10) Process payment (happy path)
Write-Host "Processing payment (happy path)..."
$pay = Invoke-Api -Method POST -Url "$CheckoutSvc/api/payments/process" -Body @{ orderId = $ORDER_ID; userId = $ORDER_USER_ID; method = "CREDIT_CARD"; amount = 200 } -AllowFailure
Write-TextLog "checkout_response" ("orderId={0} response={1}" -f $ORDER_ID, (Get-ShortSummary $pay)) | Out-Null

# 11) Capture RabbitMQ messages (after payment)
Write-Host "Capturing RabbitMQ messages (after payment)..."
Capture-RabbitQueues -Queues $RabbitQueues -Count 200

# 12) Poll until PAID
Write-Host "Polling order until PAID..."
try {
    $orderFinalStatus = Poll-OrderStatus -orderId $ORDER_ID -expectedStatus "PAID" -Interval $PollIntervalSeconds -Timeout $PollTimeoutSeconds
    Write-TextLog "order_final" ("orderId={0} status={1}" -f $ORDER_ID, $orderFinalStatus) | Out-Null
} catch {
    Write-Host "Timeout waiting for PAID: $_"
}

Write-Host "Done. Logs: $LogDir"