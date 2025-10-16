resource "aws_lambda_function" "this" {
  function_name = "HelloFunction"
  handler       = "example.Hello::handleRequest"
  runtime       = var.runtime
  role          = var.role_arn

  filename         = var.jar_file_path
  source_code_hash = filebase64sha256(var.jar_file_path)

  lifecycle {
    create_before_destroy = true
  }
}
resource "aws_lambda_function" "add_item" {
  function_name = "DynamodbAddItemFunction"
  handler       = "example.DynamodbAddItemHandler::handleRequest"
  runtime       = var.runtime
  role          = var.role_arn

  filename         = var.jar_file_path
  source_code_hash = filebase64sha256(var.jar_file_path)

  memory_size = 512
  timeout     = 30
}

resource "aws_lambda_function" "list_items_by_gsi" {
  function_name = "DynamodbListItemsFunction"
  runtime       = var.runtime
  handler       = "example.DynamodbListItemsHandler::handleRequest"
  role          = var.role_arn

  filename         = var.jar_file_path
  source_code_hash = filebase64sha256(var.jar_file_path)

  memory_size = 512
  timeout     = 30
}

resource "aws_lambda_function" "edit_item" {
  function_name = "DynamodbEditItemFunction"
  runtime       = var.runtime
  handler       = "example.DynamodbEditItemHandler::handleRequest"
  role          = var.role_arn

  filename         = var.jar_file_path
  source_code_hash = filebase64sha256(var.jar_file_path)

  memory_size = 512
  timeout     = 30
}