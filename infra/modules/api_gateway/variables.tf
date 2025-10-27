variable "api_name" {
  type        = string
  description = "Nome da API Gateway"
}

variable "api_description" {
  type    = string
  default = "API REST gerenciada via Terraform"
}

variable "lambda_list_lists_uri" {
  description = "URI da função Lambda para listar listas"
  type        = string
}

variable "lambda_create_list_uri" {
  description = "URI da função Lambda para criar lista"
  type        = string
}

variable "lambda_get_list_uri" {
  description = "URI da função Lambda para buscar uma lista"
  type        = string
}

variable "lambda_edit_list_uri" {
  description = "URI da função Lambda para editar uma lista"
  type        = string
}

variable "lambda_list_items_uri" {
  description = "URI da função Lambda para listar itens"
  type        = string
}

variable "lambda_add_item_uri" {
  description = "URI da função Lambda para adicionar item"
  type        = string
}

variable "lambda_edit_item_uri" {
  description = "URI da função Lambda para editar item"
  type        = string
}

variable "lambda_delete_item_uri" {
  description = "URI da função Lambda para deletar item"
  type        = string
}

variable "stage_name" {
  description = "Nome do estágio da API"
  type        = string
}

variable "aws_region" {
  description = "Região AWS para compor a URL pública da API"
  type        = string
}

variable "user_pool_arn" {
  description = "ARN do Cognito User Pool para usar no authorizer"
  type        = string
}