# modules/cognito/outputs.tf
output "user_pool_arn" {
  description = "ARN do User Pool criado"
  value       = aws_cognito_user_pool.pool.arn
}