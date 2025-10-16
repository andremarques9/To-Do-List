variable "aws_region" {
  description = "Região AWS para deploy das Lambdas"
  type        = string
}

variable "runtime" {
  type        = string
  default     = "java21"
  description = "Runtime da Lambda"
}

variable "role_arn" {
  type        = string
  description = "ARN da IAM Role com permissões para Lambda"
}

variable "jar_file_path" {
  type        = string
  description = "Caminho local para o .jar empacotado da função"
}

variable "table_name" {
  description = "Nome da tabela DynamoDB"
  type        = string
}

variable "api_gateway_execution_arn" {
  description = "Execution ARN da API Gateway que invoca a Lambda"
  type        = string
}