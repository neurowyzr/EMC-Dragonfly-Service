package com.neurowyzr.nw.dragon.service

import io.getquill.{MysqlJdbcContext, SnakeCase}

package object data {

  final type CoreSqlDbContext = MysqlJdbcContext[SnakeCase]

  final type DragonSqlDbContext = MysqlJdbcContext[SnakeCase]
}
