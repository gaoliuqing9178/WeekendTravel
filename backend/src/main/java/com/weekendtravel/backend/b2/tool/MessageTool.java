package com.weekendtravel.backend.b2.tool;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MessageTool {

    private static final String TEMPLATE_VERSION = "fallback-v1";
    private static final Set<String> VALID_SCENARIOS = Set.of("family", "friends");

    public MessageResult execute(MessageRequest request) {
        return composeShareMessage(request);
    }

    public MessageResult composeShareMessage(MessageRequest request) {
        long start = System.currentTimeMillis();
        MessageRequest normalized = normalize(request);
        String shareMessage = buildMessage(normalized);
        MessagePlanPayload plan = new MessagePlanPayload(
                normalized.planId(),
                normalized.scenario(),
                normalized.status(),
                Boolean.TRUE.equals(normalized.isPlanB()),
                normalized.planBReason(),
                normalized.summary(),
                sortedTimeline(normalized.timeline()),
                normalized.actions(),
                shareMessage,
                normalized.totalDurationHours() == null ? 0.0 : normalized.totalDurationHours(),
                normalized.replanCount() == null ? 0 : normalized.replanCount(),
                normalized.createdAt()
        );

        return new MessageResult(
                normalized.planId(),
                normalized.scenario(),
                shareMessage,
                plan,
                false,
                TEMPLATE_VERSION,
                System.currentTimeMillis() - start
        );
    }

    private MessageRequest normalize(MessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("message request is required");
        }
        String planId = requireText(request.planId(), "planId");
        String scenario = requireText(request.scenario(), "scenario").toLowerCase(Locale.ROOT);
        if (!VALID_SCENARIOS.contains(scenario)) {
            throw new IllegalArgumentException("scenario must be family or friends");
        }
        if (request.timeline() == null || request.timeline().isEmpty()) {
            throw new IllegalArgumentException("timeline is required");
        }

        return new MessageRequest(
                planId,
                scenario,
                emptyToDefault(request.status(), "CONFIRM"),
                request.isPlanB(),
                optionalText(request.planBReason()),
                optionalText(request.summary()),
                sortedTimeline(request.timeline()),
                request.actions(),
                request.totalDurationHours(),
                request.replanCount(),
                optionalText(request.createdAt())
        );
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String emptyToDefault(String value, String fallback) {
        String text = optionalText(value);
        return text == null ? fallback : text;
    }

    private List<MessageTimeSlot> sortedTimeline(List<MessageTimeSlot> timeline) {
        List<IndexedSlot> indexed = new ArrayList<>();
        for (int index = 0; index < timeline.size(); index++) {
            indexed.add(new IndexedSlot(index, timeline.get(index)));
        }
        return indexed.stream()
                .sorted(Comparator.comparingInt(this::sortOrder)
                        .thenComparingInt(IndexedSlot::index))
                .map(IndexedSlot::slot)
                .toList();
    }

    private int sortOrder(IndexedSlot indexedSlot) {
        int order = indexedSlot.slot().order();
        return order <= 0 ? Integer.MAX_VALUE : order;
    }

    private String buildMessage(MessageRequest request) {
        List<String> parts = new ArrayList<>();
        parts.add(buildTimelineText(request.timeline()));

        String actionText = buildActionText(request.actions());
        if (!actionText.isBlank()) {
            parts.add(actionText);
        }

        if (Boolean.TRUE.equals(request.isPlanB()) && request.planBReason() != null) {
            parts.add("Plan B 说明：" + request.planBReason() + "。");
        }

        parts.add(buildScenarioTail(request));

        String durationText = buildDurationText(request.totalDurationHours());
        if (!durationText.isBlank()) {
            parts.add(durationText);
        }

        return String.join("", parts);
    }

    private String buildTimelineText(List<MessageTimeSlot> timeline) {
        List<String> fragments = new ArrayList<>();
        for (int index = 0; index < timeline.size(); index++) {
            MessageTimeSlot slot = timeline.get(index);
            String place = preferredPlaceName(slot);
            String time = optionalText(slot.startTime());
            if (index == 0) {
                fragments.add(time == null
                        ? "今天下午先去" + place
                        : "今天下午 " + time + " 先去" + place);
            } else {
                fragments.add(time == null
                        ? "再去" + place
                        : time + " 去" + place);
            }
        }
        return String.join("，", fragments) + "。";
    }

    private String preferredPlaceName(MessageTimeSlot slot) {
        String title = optionalText(slot.title());
        if (title != null) {
            return title;
        }
        String poiName = optionalText(slot.poiName());
        return poiName == null ? "下一站" : poiName;
    }

    private String buildActionText(List<MessageActionSummary> actions) {
        List<String> descriptions = actions.stream()
                .filter(action -> action != null)
                .filter(action -> !"send_message".equals(action.actionType()))
                .map(MessageActionSummary::description)
                .filter(description -> description != null && !description.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (descriptions.isEmpty()) {
            return "";
        }
        return "已安排：" + String.join("、", descriptions) + "。";
    }

    private String buildScenarioTail(MessageRequest request) {
        if ("family".equals(request.scenario())) {
            return familyTail(request.timeline());
        }
        return friendsTail(request.timeline());
    }

    private String familyTail(List<MessageTimeSlot> timeline) {
        String notes = notesText(timeline);
        if (containsAny(notes, "低卡", "少油", "轻食", "减肥")) {
            return "整体照顾孩子体力和低负担饮食，路线尽量轻松。";
        }
        if (containsAny(notes, "少步行", "不太累", "低体力")) {
            return "整体照顾孩子体力，少步行、不太累。";
        }
        return "整体节奏偏轻松，适合带孩子一起走。";
    }

    private String friendsTail(List<MessageTimeSlot> timeline) {
        String notes = notesText(timeline);
        if (containsAny(notes, "拍照", "照片", "氛围")) {
            return "整体兼顾玩、吃、拍照和聊天，路线比较轻松。";
        }
        return "整体兼顾玩、吃和聊天，适合朋友局。";
    }

    private String notesText(List<MessageTimeSlot> timeline) {
        StringBuilder builder = new StringBuilder();
        for (MessageTimeSlot slot : timeline) {
            builder.append(' ')
                    .append(nullToBlank(slot.type()))
                    .append(' ')
                    .append(nullToBlank(slot.title()))
                    .append(' ')
                    .append(nullToBlank(slot.poiName()));
            for (String note : slot.notes()) {
                builder.append(' ').append(nullToBlank(note));
            }
        }
        return builder.toString();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String buildDurationText(Double totalDurationHours) {
        if (totalDurationHours == null || totalDurationHours <= 0) {
            return "";
        }
        return "全程约" + formatHours(totalDurationHours) + "小时。";
    }

    private String formatHours(double hours) {
        if (Math.abs(hours - Math.rint(hours)) < 0.01) {
            return String.valueOf((int) Math.rint(hours));
        }
        return String.format(Locale.ROOT, "%.1f", hours);
    }

    private record IndexedSlot(int index, MessageTimeSlot slot) {
    }
}
