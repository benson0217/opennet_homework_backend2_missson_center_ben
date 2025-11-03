package com.example.demo.user.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 使用者聚合根
 * 任務中心系統中的使用者及其相關行為。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private Long id;

    private String username;

    private Integer points;

    private LocalDateTime registrationDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     *
     * @param username 使用者名稱
     * @return 新建立的使用者實體
     * @throws IllegalArgumentException 如果使用者名稱為空
     */
    public static User create(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("使用者名稱不能為空");
        }

        User user = new User();
        user.username = username.trim();
        user.points = 0;
        user.registrationDate = LocalDateTime.now();
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    /**
     * 為使用者帳戶增加點數。
     *
     * @param pointsToAdd 要增加的點數
     * @throws IllegalArgumentException 如果點數為負數
     */
    public void addPoints(int pointsToAdd) {
        if (pointsToAdd < 0) {
            throw new IllegalArgumentException("不能增加負數點數");
        }
        this.points += pointsToAdd;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 檢查使用者是否在註冊後的30天內。
     *
     * @return 如果在30天內，則返回 true
     */
    public boolean isWithinThirtyDayPeriod() {
        return registrationDate != null &&
               registrationDate.plusDays(30).isAfter(LocalDateTime.now());
    }

    /**
     * 檢查使用者是否有資格參與任務。
     *
     * @return 如果有資格，則返回 true
     */
    public boolean isEligibleForMissions() {
        // 目前的資格條件與是否在30天內相同，未來可能擴充
        return isWithinThirtyDayPeriod();
    }
}
