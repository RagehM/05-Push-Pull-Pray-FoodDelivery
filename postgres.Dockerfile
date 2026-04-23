FROM postgres:17

ENV POSTGRES_DB=fooddeliverydb
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres

EXPOSE 5432


RUN apt update && apt install -y python3 python3-venv python3-pip
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

#Keep install at the top, so that it doesn't get cached and re-run every time we change the code

RUN pip install --upgrade pip
RUN pip install javalang psycopg2-binary

COPY ./automation/* /automation/

COPY ./checkout-service/src/main/java/com/team05/fooddelivery/checkout/enums /enums/checkout
COPY ./delivery-service/src/main/java/com/team05/fooddelivery/delivery/enums /enums/delivery
COPY ./order-service/src/main/java/com/team05/fooddelivery/order/enums /enums/order
COPY ./restaurant-service/src/main/java/com/team05/fooddelivery/restaurant/enums /enums/restaurant
COPY ./user-service/src/main/java/com/team05/fooddelivery/user/enums /enums/user

#Check so that it can run on windows
RUN sed -i 's/\r$//' /automation/pg_docker_run.sh

RUN chmod +x /automation/pg_docker_run.sh

CMD /automation/pg_docker_run.sh