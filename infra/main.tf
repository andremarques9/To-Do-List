terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_iam_role" "lambda_exec" {
  name = "lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Principal = { Service = "lambda.amazonaws.com" }
      Effect    = "Allow"
    }]
  })
}

resource "aws_iam_role_policy" "lambda_ddb_policy" {
  name = "lambda-ddb-policy"
  role = "lambda-exec-role"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["dynamodb:PutItem"]
        Resource = data.aws_dynamodb_table.existing.arn
      },
      {
        Effect = "Allow"
        Action = ["dynamodb:Query"]
        Resource = [
          data.aws_dynamodb_table.existing.arn,
          "${data.aws_dynamodb_table.existing.arn}/index/gsi-pedidos"
        ]
      },
      {
        Effect = "Allow"
        Action = ["dynamodb:UpdateItem"]
        Resource = [
          data.aws_dynamodb_table.existing.arn
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "basic_exec" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

module "lambda" {
  source                    = "./modules/lambda"
  aws_region                = var.aws_region
  role_arn                  = aws_iam_role.lambda_exec.arn
  jar_file_path             = var.jar_file_path
  table_name                = var.dynamodb_table_name
  api_gateway_execution_arn = module.api_gateway.execution_arn
}

module "dynamodb" {
  source                   = "./modules/dynamodb"
  table_name               = var.dynamodb_table_name
  partition_key            = var.dynamodb_partition_key
  sort_key                 = var.dynamodb_sort_key
  billing_mode             = var.dynamodb_billing_mode
  global_secondary_indexes = var.dynamodb_global_secondary_indexes
}

data "aws_dynamodb_table" "existing" {
  name = var.dynamodb_table_name
}

module "api_gateway" {
  source           = "./modules/api_gateway"
  aws_region       = var.aws_region
  api_name         = "apiREST"
  stage_name       = "prod"
  lambda_list_lists_uri   = module.lambda.list_lists_invoke_arn
  lambda_create_list_uri  = module.lambda.create_list_invoke_arn
  lambda_get_list_uri     = module.lambda.get_list_invoke_arn
  lambda_edit_list_uri    = module.lambda.edit_list_invoke_arn
  lambda_list_items_uri   = module.lambda.list_items_invoke_arn
  lambda_add_item_uri     = module.lambda.add_item_invoke_arn
  lambda_edit_item_uri    = module.lambda.edit_item_invoke_arn
  lambda_delete_item_uri  = module.lambda.delete_item_invoke_arn
  user_pool_arn    = module.cognito.user_pool_arn
}

module "cognito" {
  source = "./modules/cognito"
}

resource "aws_lambda_permission" "allow_apigw_add_item" {
  statement_id  = "AllowAPIGWInvokeAddItem"
  action        = "lambda:InvokeFunction"
  function_name = module.lambda.add_item_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${module.api_gateway.execution_arn}/*/POST"
}

resource "aws_lambda_permission" "allow_apigw_list_items" {
  statement_id  = "AllowAPIGWInvokeListItems"
  action        = "lambda:InvokeFunction"
  function_name = module.lambda.list_items_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${module.api_gateway.execution_arn}/*/GET"
}

resource "aws_lambda_permission" "allow_apigw_edit_item" {
  statement_id  = "AllowAPIGWInvokeEditItem"
  action        = "lambda:InvokeFunction"
  function_name = module.lambda.edit_item_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${module.api_gateway.execution_arn}/*/PATCH"
}
resource "aws_lambda_permission" "allow_post" {
  statement_id  = "AllowAPIGWInvokePost"
  action        = "lambda:InvokeFunction"
  function_name = module.lambda.add_item_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${module.api_gateway.execution_arn}/*/POST"
}

resource "aws_lambda_permission" "allow_get" {
  statement_id  = "AllowAPIGWInvokeGet"
  action        = "lambda:InvokeFunction"
  function_name = module.lambda.list_items_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${module.api_gateway.execution_arn}/*/GET"
}

resource "aws_lambda_permission" "allow_patch" {
  statement_id  = "AllowAPIGWInvokePatch"
  action        = "lambda:InvokeFunction"
  function_name = module.lambda.edit_item_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${module.api_gateway.execution_arn}/*/PATCH"
}