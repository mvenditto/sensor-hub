package api.services.security.permission

import java.security.BasicPermission

case class DriverManagementPermission(action: String) extends BasicPermission(action)
