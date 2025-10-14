variable "api_name" {
  type        = string
  description = "Nome da API Gateway"
}

variable "api_description" {
  type    = string
  default = "API REST gerenciada via Terraform"
}

variable "lambda_post_uri" {
  type        = string
  description = "Invoke ARN da Lambda POST"
}

variable "lambda_get_uri" {
  type        = string
  description = "Invoke ARN da Lambda GET"
}

variable "lambda_patch_uri" {
  type        = string
  description = "Invoke ARN da Lambda PATCH"
}

variable "stage_name" {
  type    = string
  default = "prod"
}

variable "aws_region" {
  description = "Região AWS para compor a URL pública da API"
  type        = string
}

variable "user_pool_arn" {
  description = "ARN do Cognito User Pool para usar no authorizer"
  type        = string
}