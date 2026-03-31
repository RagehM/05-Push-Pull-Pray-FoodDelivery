import time

import javalang
import psycopg2
from psycopg2 import sql
import os

def load_enum_into_dicts(isRunningInDocker: bool):

    root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    enum_dict:dict = {}

    service_list:list = ["checkout", "delivery",  "order", "restaurant", "user"]

    if isRunningInDocker:
        root_dir = "/enums"

        for service in service_list:
            for filename in os.listdir(root_dir + '/' + service):
                if filename.endswith(".java"):
                    with open(os.path.join(root_dir, service, filename), "r") as enumFile:
                        fileContent = enumFile.read()
                        parseTree = javalang.parse.parse(fileContent)
                        for type in parseTree.types:
                            if isinstance(type, javalang.tree.EnumDeclaration):
                                print(f"    Parsing enum: {type.name}")
                                enum_dict[type.name] = [constant.name for constant in type.body.constants]
                                print(f"        Constants: {enum_dict[type.name]}")


        return enum_dict


    for service in service_list:
        enum_filepath: str = os.path.join(root_dir, service+"-service", "src", "main", "java", "com", "team05" , "fooddelivery", service, "enums")

        if not os.path.exists(enum_filepath):
            print(f"Enum directory not found for service: {service} 🔍")
            continue

        print(f"Processing enums for service: {service} ✅")

        for filename in os.listdir(enum_filepath):
            if filename.endswith(".java"):
                with open(os.path.join(enum_filepath, filename), "r") as enumFile:
                    fileContent = enumFile.read()
                    parseTree = javalang.parse.parse(fileContent)
                    
                    for type in parseTree.types:
                        if isinstance(type, javalang.tree.EnumDeclaration):
                            print(f"    Parsing enum: {type.name}")
                            enum_dict[type.name] = [constant.name for constant in type.body.constants]
                            print(f"        Constants: {enum_dict[type.name]}")


    return enum_dict

def connect_to_db(retries: int = 100, delay: int = 5):

    for _ in range(retries):
        try:
            conn = psycopg2.connect(
                host=  "fooddelivery-db" if isRunningInDocker() else "localhost",
                database=os.getenv("POSTGRES_DB", "fooddeliverydb"),
                user=os.getenv("POSTGRES_USER", "postgres"),
                password=os.getenv("POSTGRES_PASSWORD", "postgres")
            )
            return conn
        except psycopg2.Error as e:
            print(f"Error connecting to database: {e}Retrying in {delay} seconds...\n")
            time.sleep(delay)
    raise Exception("Failed to connect to database")

def verify_enums(enum_dict: dict):
    conn = connect_to_db()
    existing_enums = fetch_existing_enums_from_db(conn)

    for key, value in enum_dict.items():
        if key.lower() in existing_enums:
            print(f"ENUM '{key}' already exists, skipping...")
            continue

        create_enum(conn, key, value)

    conn.close()

def create_enum(conn, enum_name: str, enum_values):
    cursor = conn.cursor()
    creation_query = sql.SQL(
        """
        CREATE TYPE {enum} AS ENUM ({values});
        """
    ).format(
        enum=sql.Identifier(enum_name.lower()),
        values=sql.SQL(', ').join(sql.Literal(val) for val in enum_values)
    )
    cursor.execute(creation_query)
    conn.commit()
    cursor.close()
    print(f"ENUM '{enum_name}' has been created")
    

def fetch_existing_enums_from_db(conn):
    cursor = conn.cursor()
    cursor.execute("""
                   SELECT DISTINCT
                        t.typname
                    FROM pg_type t
                    JOIN pg_enum e ON t.oid = e.enumtypid
                    JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
                   """)
    enum_list = [x[0] for x in cursor.fetchall()]
    # print(enumList)
    cursor.close()
    return enum_list

def isRunningInDocker() -> bool:
    return os.path.exists("/.dockerenv")

if __name__ == "__main__":

    enums_dict = load_enum_into_dicts(isRunningInDocker())

    print("============================Enums Loaded=============================")

    verify_enums(enums_dict)

    open("/tmp/healthy", "w").close()