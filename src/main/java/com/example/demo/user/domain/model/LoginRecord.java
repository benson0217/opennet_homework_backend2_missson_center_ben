package com.example.demo.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 登入記錄實體
 * 表示一次使用者登入記錄，用於追蹤連續登入等行為。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LoginRecord {
    
    private Long id;
    
    private Long userId;
    
    private LocalDate loginDate;
    
    private LocalDateTime loginTime;
    
    private LocalDateTime createdAt;

    /**
     *
     * @param userId    使用者ID
     * @param loginDate 登入日期
     * @return 新建立的登入記錄實體
     * @throws IllegalArgumentException 如果使用者ID無效或登入日期為 null
     */
    public static LoginRecord create(Long userId, LocalDate loginDate) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("無效的使用者ID");
        }
        if (loginDate == null) {
            throw new IllegalArgumentException("登入日期不能為空");
        }
        
        LoginRecord loginRecord = new LoginRecord();
        loginRecord.userId = userId;
        loginRecord.loginDate = loginDate;
        loginRecord.loginTime = LocalDateTime.now();
        loginRecord.createdAt = LocalDateTime.now();
        return loginRecord;
    }

    /**
     * 替今天建立一個新的登入記錄。
     *
     * @param userId 使用者ID
     * @return 為今天建立的登入記錄實體
     */
    public static LoginRecord createForToday(Long userId) {
        return create(userId, LocalDate.now());
    }

    /**
     * 檢查此登入記錄是否為今天。
     *
     * @return 如果登入日期是今天，則返回 true
     */
    public boolean isToday() {
        return loginDate != null && loginDate.equals(LocalDate.now());
    }
}
