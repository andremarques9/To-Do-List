resource "aws_dynamodb_table" "main" {
  name         = var.table_name
  billing_mode = var.billing_mode

  # Primary Key
  hash_key  = var.partition_key.name
  range_key = var.sort_key.name

  attribute {
    name = var.partition_key.name
    type = var.partition_key.type
  }

  attribute {
    name = var.sort_key.name
    type = var.sort_key.type
  }
}