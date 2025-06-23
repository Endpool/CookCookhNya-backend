package db

import com.augustnagro.magnum.SqlNameMapper

/**
 * - Column names: Convert `CamelCase` field names to `snake_case` (e.g., `userId` → `user_id`).
 * - Table names:
 *   1. Strip the `"Db"` prefix if present (e.g., `DbUser` → `User`).
 *   2. Add an `"s"` suffix for pluralization (e.g., `User` → `Users`).
 *   3. Convert the result to `snake_case` (e.g., `Users` → `users`).
 */
object CustomSqlNameMapper extends SqlNameMapper:
  def toColumnName(scalaName: String): String =
    SqlNameMapper.CamelToSnakeCase.toColumnName(scalaName)

  def toTableName(scalaName: String): String =
    SqlNameMapper.CamelToSnakeCase.toTableName(scalaName.stripPrefix("Db") ++ "s")
