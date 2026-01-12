package com.vortexia.core.command;

/**
 * Interface cơ sở cho tất cả các lệnh của Vortexia.
 * Giúp việc quản lý và đăng ký lệnh trở nên có hệ thống hơn.
 */
public interface BaseCommand {
  /**
   * Phương thức thực hiện đăng ký lệnh với CommandAPI.
   */
  void register();
}
