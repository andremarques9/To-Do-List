output "lambda_function_name" {
  value = aws_lambda_function.this.function_name
}

output "lambda_function_arn" {
  value = aws_lambda_function.this.arn
}

output "add_item_invoke_arn" {
  description = "Invoke ARN da Lambda de criação"
  value       = aws_lambda_function.add_item.invoke_arn
}

output "list_items_invoke_arn" {
  description = "Invoke ARN da Lambda de listagem"
  value       = aws_lambda_function.list_items_by_gsi.invoke_arn
}

output "edit_item_invoke_arn" {
  description = "Invoke ARN da Lambda de edição"
  value       = aws_lambda_function.edit_item.invoke_arn
}

output "add_item_function_name" {
  description = "Nome da função Lambda de criação"
  value       = aws_lambda_function.add_item.function_name
}

output "list_items_function_name" {
  description = "Nome da função Lambda de listagem"
  value       = aws_lambda_function.list_items_by_gsi.function_name
}

output "edit_item_function_name" {
  description = "Nome da função Lambda de edição"
  value       = aws_lambda_function.edit_item.function_name
}