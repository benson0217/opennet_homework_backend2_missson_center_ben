package com.example.demo.user.domain.service;

import com.example.demo.user.domain.model.LoginRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 使用者領域服務
 */
@Service
public class UserDomainService {

    /**
     * 從登入記錄中計算連續登入天數。
     * 返回截至最近一次登入的連續天數。
     *
     * @param loginRecords 登入記錄列表，應按日期降序排序
     * @return 連續登入的天數
     */
    public int calculateConsecutiveLoginDays(List<LoginRecord> loginRecords) {
        if (loginRecords == null || loginRecords.isEmpty()) {
            return 0;
        }

        List<LocalDate> sortedDates = loginRecords.stream()
            .map(LoginRecord::getLoginDate)
            .distinct()
            .sorted((a, b) -> b.compareTo(a))
            .toList();

        if (sortedDates.isEmpty()) {
            return 0;
        }

        int consecutiveDays = 1;
        LocalDate previousDate = sortedDates.getFirst();

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate currentDate = sortedDates.get(i);
            if (currentDate.plusDays(1).equals(previousDate)) {
                consecutiveDays++;
                previousDate = currentDate;
            } else {
                break;
            }
        }

        return consecutiveDays;
    }
}
