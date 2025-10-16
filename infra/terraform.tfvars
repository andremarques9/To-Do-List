aws_region = "us-east-2"

function_name = "MyLambda"
handler       = "com.exemplo.Handler::handleRequest"
role_arn      = "arn:aws:iam::123456789012:role/lambda-exec-role"
jar_file_path = "C:/Users/amd50/IdeaProjects/To-Do-List/target/untitled-1.0-SNAPSHOT.jar"

dynamodb_table_name    = "MySingleTable"
dynamodb_partition_key = { name = "PK", type = "S" }
dynamodb_sort_key      = { name = "SK", type = "S" }
dynamodb_billing_mode  = "PAY_PER_REQUEST"
dynamodb_global_secondary_indexes = [
  {
    name            = "gsi-pedidos"
    hash_key        = "GSI1PK"
    range_key       = "GSI1SK"
    projection_type = "ALL"
  }
]