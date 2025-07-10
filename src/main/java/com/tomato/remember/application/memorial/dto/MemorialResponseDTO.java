package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.memorial.code.AiTrainingStatus;
import com.tomato.remember.application.memorial.code.InterestType;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 메모리얼 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialResponseDTO {

    private Long id;

    private String name;

    private String nickname;

    private Gender gender;

    private LocalDate birthDate;

    private LocalDate deathDate;

    private Relationship relationship;

    private List<InterestType> interests;

    private String profileImageUrl;

    private String voiceFileUrl;

    private String videoFileUrl;

    private String userImageUrl;

    private Boolean aiTrainingCompleted;

    private AiTrainingStatus aiTrainingStatus;

    private LocalDate lastVisitAt;

    private Integer totalVisits;

    private Integer memoryCount;

    private MemorialStatus status;

    private Boolean isPublic;

    private String additionalInfo;

    private Long ownerId;

    private String ownerName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer age;

    private String formattedAge;

    private String interestDisplayNames;

    private Integer familyMemberCount;

    private Boolean canStartVideoCall;

    private Boolean hasRequiredFiles;
}