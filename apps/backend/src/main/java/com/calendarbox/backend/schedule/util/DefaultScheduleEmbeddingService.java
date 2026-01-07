package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class DefaultScheduleEmbeddingService {

    private final PythonEmbeddingService pythonEmbeddingService;

    public float[] embedScheduleForSearch(PlaceRecommendRequest request) {
        String text = buildQueryText(request);
        return pythonEmbeddingService.embed(text);
    }

    public float[] embedScheduleEntity(Schedule schedule) {
        // 나중에 schedule_embedding 테이블 채울 때 쓸 용도
        String text = buildScheduleText(schedule);
        return pythonEmbeddingService.embed(text);
    }

    // ====== 여기부터는 텍스트 만드는 부분 ======

    private String buildQueryText(PlaceRecommendRequest req) {
        StringBuilder sb = new StringBuilder();

        if (req.title() != null && !req.title().isBlank()) {
            sb.append("제목: ").append(req.title()).append("\n");
        }

        if (req.memo() != null && !req.memo().isBlank()) {
            sb.append("메모: ").append(req.memo()).append("\n");
        }

        if (req.startAt() != null) {
            var start = req.startAt();
            var dayOfWeek = start.getDayOfWeek();
            String dayKorean = switch (dayOfWeek) {
                case MONDAY    -> "월요일";
                case TUESDAY   -> "화요일";
                case WEDNESDAY -> "수요일";
                case THURSDAY  -> "목요일";
                case FRIDAY    -> "금요일";
                case SATURDAY  -> "토요일";
                case SUNDAY    -> "일요일";
            };

            int hour = start.getHour();
            String timeSlot = timeSlotLabel(hour);

            sb.append("요일: ").append(dayKorean).append("\n");
            sb.append("시간대: ").append(timeSlot).append("\n");
            sb.append("시작시간: ")
                    .append(String.format("%02d:%02d", start.getHour(), start.getMinute()))
                    .append("\n");
        }

        if (req.endAt() != null) {
            var end = req.endAt();
            sb.append("종료시간: ")
                    .append(String.format("%02d:%02d", end.getHour(), end.getMinute()))
                    .append("\n");
        }

        if (req.startAt() != null && req.endAt() != null) {
            long minutes = Duration.between(req.startAt(), req.endAt()).toMinutes();
            if (minutes > 0) {
                sb.append("소요시간: ").append(minutes).append("분\n");
            }
        }

        if (req.participantCount() != null && req.participantCount() > 0) {
            int count = req.participantCount();
            String label = (count == 1) ? "혼자"
                    : (count <= 3) ? ("소규모 모임 (" + count + "명)")
                    : ("다인원 모임 (" + count + "명)");
            sb.append("참여인원: ").append(label).append("\n");
        }

        return sb.toString().trim();
    }

    private String buildScheduleText(Schedule schedule) {
        StringBuilder sb = new StringBuilder();
        sb.append("제목: ").append(schedule.getTitle()).append("\n");
        if (schedule.getMemo() != null) {
            sb.append("메모: ").append(schedule.getMemo()).append("\n");
        }

        ZoneId zone = ZoneId.of("Asia/Seoul");

        if (schedule.getStartAt() != null) {
            ZonedDateTime start = schedule.getStartAt().atZone(zone);
            var dayOfWeek = start.getDayOfWeek();
            String dayKorean = switch (dayOfWeek) {
                case MONDAY    -> "월요일";
                case TUESDAY   -> "화요일";
                case WEDNESDAY -> "수요일";
                case THURSDAY  -> "목요일";
                case FRIDAY    -> "금요일";
                case SATURDAY  -> "토요일";
                case SUNDAY    -> "일요일";
            };

            int hour = start.getHour();
            String timeSlot = timeSlotLabel(hour);

            sb.append("요일: ").append(dayKorean).append("\n");
            sb.append("시간대: ").append(timeSlot).append("\n");
            sb.append("시작시간: ")
                    .append(String.format("%02d:%02d", start.getHour(), start.getMinute()))
                    .append("\n");
        }

        if (schedule.getEndAt() != null) {
            ZonedDateTime end = schedule.getEndAt().atZone(zone);
            sb.append("종료시간: ")
                    .append(String.format("%02d:%02d", end.getHour(), end.getMinute()))
                    .append("\n");
        }

        if (schedule.getStartAt() != null && schedule.getEndAt() != null) {
            long minutes = Duration.between(schedule.getStartAt(), schedule.getEndAt()).toMinutes();
            if (minutes > 0) {
                sb.append("소요시간: ").append(minutes).append("분\n");
            }
        }

        int accepted = 0;
        if (schedule.getParticipants() != null) {
            for (var sp : schedule.getParticipants()) {
                if (sp.getStatus() == ScheduleParticipantStatus.ACCEPTED) accepted++;
            }
        }
        if (accepted > 0) {
            String label = (accepted == 1) ? "혼자"
                    : (accepted <= 3) ? ("소규모 모임 (" + accepted + "명)")
                    : ("다인원 모임 (" + accepted + "명)");
            sb.append("참여인원: ").append(label).append("\n");
        }
        return sb.toString().trim();
    }

    private String timeSlotLabel(int hour) {
        if (hour >= 5 && hour < 11) {
            return "아침";
        } else if (hour >= 11 && hour < 14) {
            return "점심";
        } else if (hour >= 14 && hour < 18) {
            return "오후";
        } else if (hour >= 18 && hour < 23) {
            return "저녁";
        } else {
            return "밤";
        }
    }
}
