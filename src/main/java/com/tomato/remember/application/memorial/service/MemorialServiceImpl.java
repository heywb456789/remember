package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.repository.MemberRepository;
import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialCreateResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialListResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialQuestionAnswerDTO;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.entity.MemorialAnswer;
import com.tomato.remember.application.memorial.entity.MemorialFile;
import com.tomato.remember.application.memorial.code.MemorialFileType;
import com.tomato.remember.application.memorial.entity.MemorialQuestion;
import com.tomato.remember.application.memorial.repository.MemorialAnswerRepository;
import com.tomato.remember.application.memorial.repository.MemorialQuestionRepository;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.code.StorageCategory;
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.util.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 메모리얼 서비스 구현체 (개선된 버전)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemorialServiceImpl implements MemorialService {

    private final MemorialRepository memorialRepository;
    private final FileStorageService fileStorageService;
    private final MemberRepository memberRepository;
    private final MemorialAnswerRepository memorialAnswerRepository;
    private final MemorialQuestionRepository memorialQuestionRepository;

    /**
     * ✅ 메모리얼 생성 (안전장치 추가된 버전)
     */
    @Override
    @Transactional
    public MemorialCreateResponseDTO createMemorial(
            MemorialCreateRequestDTO memorialData,
            List<MultipartFile> profileImages,
            List<MultipartFile> voiceFiles,
            MultipartFile videoFile,
            Member member) {

        log.info("메모리얼 생성 시작 - 사용자: {}, 메모리얼명: {}, 답변 수: {}",
                member.getId(), memorialData.getName(), memorialData.getAnsweredQuestionCount());

        List<String> uploadedFileUrls = new ArrayList<>();

        try {
            // ✅ 1. 사전 검증 강화
            validatePreConditions(memorialData, profileImages, voiceFiles, videoFile, member);

            // 2. 빈 답변 제거
            memorialData.removeEmptyAnswers();

            // 3. 메모리얼 엔티티 생성
            Memorial memorial = createMemorialEntity(memorialData, member);

            // 4. 프로필 이미지 파일 처리
            uploadedFileUrls.addAll(processProfileImages(memorial, profileImages, member.getId()));

            // 5. 음성 파일 처리
            uploadedFileUrls.addAll(processVoiceFiles(memorial, voiceFiles, member.getId()));

            // 6. 영상 파일 처리
            uploadedFileUrls.add(processVideoFile(memorial, videoFile, member.getId()));

            // 7. 메모리얼 저장
            Memorial savedMemorial = memorialRepository.save(memorial);

            // ✅ 8. 동적 질문 답변 처리 (강화된 검증)
            processQuestionAnswersWithValidation(savedMemorial, memorialData.getQuestionAnswers(), member);

            // ✅ 9. 최종 검증
            validateFinalResult(savedMemorial);

            log.info("메모리얼 생성 완료 - ID: {}, 이름: {}, 답변 수: {}, 파일 수: {}",
                    savedMemorial.getId(), savedMemorial.getName(),
                    memorialData.getAnsweredQuestionCount(), uploadedFileUrls.size());

            return MemorialCreateResponseDTO.success(
                    savedMemorial.getId(),
                    savedMemorial.getName(),
                    savedMemorial.getNickname()
            );

        } catch (Exception e) {
            log.error("메모리얼 생성 실패 - 사용자: {}, 오류: {}", member.getId(), e.getMessage(), e);

            // 파일 롤백 처리
            rollbackUploadedFiles(uploadedFileUrls);

            // 예외 타입별 처리
            if (e instanceof APIException) {
                throw e;
            } else if (e instanceof IllegalArgumentException) {
                throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
            } else {
                throw new APIException("메모리얼 생성에 실패했습니다.", ResponseStatus.MEMORIAL_CREATION_FAILED);
            }
        }
    }

    /**
     * ✅ 사전 검증 강화
     */
    private void validatePreConditions(MemorialCreateRequestDTO memorialData,
                                       List<MultipartFile> profileImages,
                                       List<MultipartFile> voiceFiles,
                                       MultipartFile videoFile,
                                       Member member) {

        // 회원 검증
        if (member == null || member.getId() == null) {
            throw new APIException("유효하지 않은 사용자입니다.", ResponseStatus.UNAUTHORIZED);
        }

        // 기본 정보 검증
        if (memorialData == null) {
            throw new IllegalArgumentException("메모리얼 데이터가 없습니다.");
        }

        if (memorialData.getName() == null || memorialData.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("메모리얼 이름은 필수입니다.");
        }

        if (memorialData.getNickname() == null || memorialData.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("호칭은 필수입니다.");
        }

        if (memorialData.getGender() == null) {
            throw new IllegalArgumentException("성별은 필수입니다.");
        }

        if (memorialData.getRelationship() == null) {
            throw new IllegalArgumentException("관계는 필수입니다.");
        }

        // 파일 검증
        validateFileCount(profileImages, voiceFiles, videoFile);

        // 질문 답변 검증
        validateQuestionAnswers(memorialData.getQuestionAnswers());

        log.info("사전 검증 완료 - 사용자: {}, 메모리얼: {}", member.getId(), memorialData.getName());
    }

    /**
     * ✅ 동적 질문 답변 처리 (강화된 검증)
     */
    private void processQuestionAnswersWithValidation(Memorial memorial,
                                                     List<MemorialQuestionAnswerDTO> questionAnswers,
                                                     Member member) {
        if (questionAnswers == null || questionAnswers.isEmpty()) {
            log.warn("질문 답변이 없습니다 - 메모리얼: {}", memorial.getId());
            return;
        }

        log.info("질문 답변 처리 시작 - 메모리얼: {}, 답변 수: {}",
                memorial.getId(), questionAnswers.size());

        try {
            // 활성 질문 목록 조회
            List<MemorialQuestion> activeQuestions = memorialQuestionRepository.findActiveQuestions();
            Map<Long, MemorialQuestion> questionMap = activeQuestions.stream()
                    .collect(Collectors.toMap(MemorialQuestion::getId, q -> q));

            int savedCount = 0;
            int validationErrors = 0;

            for (MemorialQuestionAnswerDTO answerDTO : questionAnswers) {
                try {
                    // 빈 답변 건너뛰기
                    if (!answerDTO.hasAnswer()) {
                        continue;
                    }

                    // 질문 존재 여부 확인
                    MemorialQuestion question = questionMap.get(answerDTO.getQuestionId());
                    if (question == null) {
                        log.warn("존재하지 않는 질문 ID: {} - 메모리얼: {}",
                                answerDTO.getQuestionId(), memorial.getId());
                        validationErrors++;
                        continue;
                    }

                    // ✅ 답변 유효성 검증
                    validateAnswerContent(answerDTO, question);

                    // 답변 엔티티 생성 및 저장
                    MemorialAnswer answer = MemorialAnswer.createOwnerAnswer(
                            memorial, member, question, answerDTO.getTrimmedAnswer());

                    memorialAnswerRepository.save(answer);
                    savedCount++;

                    log.debug("질문 답변 저장 완료 - 질문ID: {}, 답변 길이: {}자, 메모리얼: {}",
                            answerDTO.getQuestionId(), answerDTO.getAnswerLength(), memorial.getId());

                } catch (Exception e) {
                    log.error("개별 답변 처리 실패 - 질문ID: {}, 메모리얼: {}, 오류: {}",
                            answerDTO.getQuestionId(), memorial.getId(), e.getMessage());
                    validationErrors++;
                }
            }

            // ✅ 필수 질문 답변 완료 여부 확인
            validateRequiredQuestionsAnswered(memorial, activeQuestions, questionAnswers);

            log.info("질문 답변 처리 완료 - 메모리얼: {}, 저장된 답변: {}, 검증 오류: {}",
                    memorial.getId(), savedCount, validationErrors);

            if (validationErrors > 0) {
                log.warn("일부 답변 처리 중 오류 발생 - 메모리얼: {}, 오류 수: {}",
                        memorial.getId(), validationErrors);
            }

        } catch (Exception e) {
            log.error("질문 답변 처리 실패 - 메모리얼: {}, 오류: {}",
                    memorial.getId(), e.getMessage(), e);
            throw new APIException("질문 답변 처리에 실패했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * ✅ 답변 내용 유효성 검증
     */
    private void validateAnswerContent(MemorialQuestionAnswerDTO answerDTO, MemorialQuestion question) {
        String trimmedAnswer = answerDTO.getTrimmedAnswer();
        int answerLength = trimmedAnswer.length();

        // 최소 길이 검증
        if (answerLength < question.getMinLength()) {
            throw new IllegalArgumentException(
                    String.format("답변이 너무 짧습니다. 최소 %d자 이상 입력해주세요: %s (현재: %d자)",
                            question.getMinLength(), question.getQuestionText(), answerLength));
        }

        // 최대 길이 검증
        if (answerLength > question.getMaxLength()) {
            throw new IllegalArgumentException(
                    String.format("답변이 너무 깁니다. 최대 %d자 이하로 입력해주세요: %s (현재: %d자)",
                            question.getMaxLength(), question.getQuestionText(), answerLength));
        }

        // 내용 품질 검증 (기본적인 체크)
        if (trimmedAnswer.matches("^[\\s\\p{P}]*$")) {
            throw new IllegalArgumentException(
                    String.format("유효한 답변을 입력해주세요: %s", question.getQuestionText()));
        }
    }

    /**
     * ✅ 필수 질문 답변 완료 여부 확인
     */
    private void validateRequiredQuestionsAnswered(Memorial memorial,
                                                  List<MemorialQuestion> activeQuestions,
                                                  List<MemorialQuestionAnswerDTO> questionAnswers) {

        Map<Long, MemorialQuestionAnswerDTO> answerMap = questionAnswers.stream()
                .filter(MemorialQuestionAnswerDTO::hasAnswer)
                .collect(Collectors.toMap(MemorialQuestionAnswerDTO::getQuestionId, a -> a));

        List<String> missingRequiredQuestions = new ArrayList<>();

        for (MemorialQuestion question : activeQuestions) {
            if (question.getIsRequired()) {
                MemorialQuestionAnswerDTO answer = answerMap.get(question.getId());
                if (answer == null || answer.getAnswerLength() < question.getMinLength()) {
                    missingRequiredQuestions.add(question.getQuestionText());
                }
            }
        }

        if (!missingRequiredQuestions.isEmpty()) {
            throw new APIException(
                    String.format("다음 필수 질문에 답변해주세요: %s",
                            String.join(", ", missingRequiredQuestions)),
                    ResponseStatus.BAD_REQUEST);
        }
    }

    /**
     * ✅ 최종 검증
     */
    private void validateFinalResult(Memorial memorial) {
        // 메모리얼 완전성 검증
        if (!memorial.isComplete()) {
            throw new APIException("메모리얼 정보가 완전하지 않습니다.", ResponseStatus.MEMORIAL_CREATION_FAILED);
        }

        // 필수 파일 확인
        if (!memorial.hasRequiredFiles()) {
            throw new APIException("필수 파일이 모두 업로드되지 않았습니다.", ResponseStatus.MEMORIAL_CREATION_FAILED);
        }

        // 저장된 답변 수 확인
        long savedAnswerCount = memorialAnswerRepository.countCompletedAnswersByMemorial(memorial);
        if (savedAnswerCount == 0) {
            log.warn("저장된 답변이 없습니다 - 메모리얼: {}", memorial.getId());
        }

        log.info("최종 검증 완료 - 메모리얼: {}, 저장된 답변 수: {}", memorial.getId(), savedAnswerCount);
    }

    /**
     * 업로드된 파일들 롤백
     */
    private void rollbackUploadedFiles(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            try {
                fileStorageService.deleteFile(fileUrl);
                log.info("파일 롤백 완료 - URL: {}", fileUrl);
            } catch (Exception e) {
                log.error("파일 롤백 실패 - URL: {}, 오류: {}", fileUrl, e.getMessage());
            }
        }
    }

    @Override
    public ListDTO<MemorialListResponseDTO> getMyMemorials(Member member, Pageable pageable) {
        log.info("사용자 접근 가능한 메모리얼 목록 조회 (페이징) - 사용자: {}", member.getId());

        Page<Memorial> memorialPage = memorialRepository.findAccessibleMemorialsByMember(member, pageable);

        Member memberWithImages = memberRepository.findByIdWithProfileImages(member.getId());
        boolean hasRequiredProfileImages = memberWithImages.hasRequiredProfileImages();

        Page<MemorialListResponseDTO> dtoPage = memorialPage.map(memorial -> {
            boolean isOwner = memorial.getOwner().getId().equals(member.getId());
            FamilyMember familyMember = null;
            if (!isOwner) {
                familyMember = memorial.getFamilyMember(member);
            }

            return buildListResponseDTO(memorial, hasRequiredProfileImages, member, isOwner, familyMember);
        });

        ListDTO<MemorialListResponseDTO> result = ListDTO.of(dtoPage);

        log.info("사용자 접근 가능한 메모리얼 목록 조회 완료 - 사용자: {}, 총 개수: {}, 페이지: {}/{}",
            member.getId(), result.getPagination().getTotalElements(),
            result.getPagination().getCurrentPage() + 1, result.getPagination().getTotalPages());

        return result;
    }

    @Override
    public MemorialListResponseDTO getMemorial(Long memorialId, Member member) {
        log.info("메모리얼 상세 조회 - ID: {}, 사용자: {}", memorialId, member.getId());

        Memorial memorial = memorialRepository.findByIdWithFiles(memorialId)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        if (!memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        }

        Member memberWithImages = memberRepository.findByIdWithProfileImages(member.getId());
        boolean hasRequiredProfileImages = memberWithImages.hasRequiredProfileImages();

        return convertToListResponseDTO(memorial, hasRequiredProfileImages);
    }

    @Override
    @Transactional
    public void recordVisit(Long memorialId, Member member) {
        log.info("메모리얼 방문 기록 - ID: {}, 사용자: {}", memorialId, member.getId());

        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        if (!memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        }

        memorial.recordVisit();
        memorialRepository.save(memorial);

        log.info("메모리얼 방문 기록 완료 - ID: {}, 총 방문 횟수: {}", memorialId, memorial.getTotalVisits());
    }

    @Override
    public List<Memorial> findByOwner(Member owner) {
        log.info("사용자 소유 메모리얼 목록 조회 - 사용자: {}", owner.getId());
        return memorialRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    @Override
    public Memorial findById(Long memorialId) {
        log.info("메모리얼 ID로 조회 - ID: {}", memorialId);
        return memorialRepository.findById(memorialId)
            .orElseThrow(() -> {
                log.warn("메모리얼을 찾을 수 없음 - ID: {}", memorialId);
                return new IllegalArgumentException("메모리얼을 찾을 수 없습니다.");
            });
    }

    @Override
    public List<MemorialListResponseDTO> getMyMemorialsForApi(Member member) {
        log.info("사용자 접근 가능한 메모리얼 목록 조회 (API용) - 사용자: {}", member.getId());

        List<Memorial> memorials = memorialRepository.findAccessibleMemorialsByMember(member);

        Member memberWithImages = memberRepository.findByIdWithProfileImages(member.getId());
        boolean hasRequiredProfileImages = memberWithImages.hasRequiredProfileImages();

        return memorials.stream()
            .map(memorial -> convertToListResponseDTO(memorial, hasRequiredProfileImages, member))
            .collect(Collectors.toList());
    }

    // 기존 메서드들은 그대로 유지
    private Memorial createMemorialEntity(MemorialCreateRequestDTO dto, Member member) {
        return Memorial.builder()
                .name(dto.getName())
                .nickname(dto.getNickname())
                .gender(dto.getGender())
                .relationship(dto.getRelationship())
                .birthDate(dto.getBirthDate())
                .deathDate(dto.getDeathDate())
                .description(dto.getDescription())
                .isPublic(dto.getIsPublic())
                .owner(member)
                .build();
    }

    private List<String> processProfileImages(Memorial memorial, List<MultipartFile> profileImages, Long memberId) {
        if (profileImages == null || profileImages.size() != 5) {
            throw new APIException("프로필 이미지는 정확히 5개가 필요합니다.", ResponseStatus.PROFILE_IMAGE_LIMIT_EXCEEDED);
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (int i = 0; i < profileImages.size(); i++) {
            MultipartFile file = profileImages.get(i);

            if (file.isEmpty()) {
                throw new APIException("빈 파일은 업로드할 수 없습니다.", ResponseStatus.FILE_EMPTY);
            }

            try {
                String fileUrl = fileStorageService.saveProfileImage(file, memberId, i + 1);
                uploadedUrls.add(fileUrl);

                MemorialFile memorialFile = MemorialFile.createProfileImage(
                    memorial, fileUrl, file.getOriginalFilename(), file.getSize(), file.getContentType(), i + 1);

                memorial.addFile(memorialFile);

                log.debug("프로필 이미지 파일 처리 완료 - 순서: {}, URL: {}", i + 1, fileUrl);

            } catch (Exception e) {
                log.error("프로필 이미지 업로드 실패 - 순서: {}, 파일: {}", i + 1, file.getOriginalFilename(), e);
                throw new APIException("프로필 이미지 업로드에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
            }
        }

        log.info("프로필 이미지 처리 완료 - 개수: {}", profileImages.size());
        return uploadedUrls;
    }

    private List<String> processVoiceFiles(Memorial memorial, List<MultipartFile> voiceFiles, Long memberId) {
        if (voiceFiles == null || voiceFiles.size() != 3) {
            throw new APIException("음성 파일은 정확히 3개가 필요합니다.", ResponseStatus.VOICE_FILE_LIMIT_EXCEEDED);
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (int i = 0; i < voiceFiles.size(); i++) {
            MultipartFile file = voiceFiles.get(i);

            if (file.isEmpty()) {
                throw new APIException("빈 파일은 업로드할 수 없습니다.", ResponseStatus.FILE_EMPTY);
            }

            try {
                String relativePath = fileStorageService.upload(file, StorageCategory.MEMORIAL, memberId);
                String fileUrl = fileStorageService.toAbsoluteUrl(relativePath);
                uploadedUrls.add(fileUrl);

                MemorialFile memorialFile = MemorialFile.createVoiceFile(
                    memorial, fileUrl, file.getOriginalFilename(), file.getSize(), file.getContentType(), i + 1);

                memorial.addFile(memorialFile);

                log.debug("음성 파일 처리 완료 - 순서: {}, URL: {}", i + 1, fileUrl);

            } catch (Exception e) {
                log.error("음성 파일 업로드 실패 - 순서: {}, 파일: {}", i + 1, file.getOriginalFilename(), e);
                throw new APIException("음성 파일 업로드에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
            }
        }

        log.info("음성 파일 처리 완료 - 개수: {}", voiceFiles.size());
        return uploadedUrls;
    }

    private String processVideoFile(Memorial memorial, MultipartFile videoFile, Long memberId) {
        if (videoFile == null || videoFile.isEmpty()) {
            throw new APIException("영상 파일은 필수입니다.", ResponseStatus.FILE_EMPTY);
        }

        try {
            String relativePath = fileStorageService.uploadVideo(videoFile, StorageCategory.MEMORIAL, memberId);
            String fileUrl = fileStorageService.toAbsoluteUrl(relativePath);

            MemorialFile memorialFile = MemorialFile.createVideoFile(
                memorial, fileUrl, videoFile.getOriginalFilename(), videoFile.getSize(), videoFile.getContentType());

            memorial.addFile(memorialFile);

            log.info("영상 파일 처리 완료 - 파일명: {}, URL: {}", videoFile.getOriginalFilename(), fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("영상 파일 업로드 실패 - 파일: {}", videoFile.getOriginalFilename(), e);
            throw new APIException("영상 파일 업로드에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
        }
    }

    // 기존 DTO 변환 메서드들도 그대로 유지
    private MemorialListResponseDTO convertToListResponseDTO(Memorial memorial, boolean hasRequiredProfileImages, Member currentUser) {
        boolean isOwner = memorial.getOwner().getId().equals(currentUser.getId());
        boolean canAccess = memorial.canBeViewedBy(currentUser);

        return MemorialListResponseDTO.builder()
            .memorialId(memorial.getId())
            .name(memorial.getName())
            .nickname(memorial.getNickname())
            .mainProfileImageUrl(memorial.getMainProfileImageUrl())
            .lastVisitAt(memorial.getLastVisitAt())
            .totalVisits(memorial.getTotalVisits())
            .memoryCount(memorial.getMemoryCount())
            .aiTrainingCompleted(memorial.getAiTrainingCompleted())
            .formattedAge(memorial.getFormattedAge())
            .relationshipDescription(memorial.getRelationship().getDisplayName())
            .canStartVideoCall(memorial.canStartVideoCall())
            .hasRequiredFiles(memorial.hasRequiredFiles())
            .hasRequiredProfileImages(hasRequiredProfileImages)
            .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
            .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
            .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))
            .isOwner(isOwner)
            .canAccess(canAccess)
            .build();
    }

    private MemorialListResponseDTO convertToListResponseDTO(Memorial memorial, boolean hasRequiredProfileImages) {
        return MemorialListResponseDTO.builder()
            .memorialId(memorial.getId())
            .name(memorial.getName())
            .nickname(memorial.getNickname())
            .mainProfileImageUrl(memorial.getMainProfileImageUrl())
            .lastVisitAt(memorial.getLastVisitAt())
            .totalVisits(memorial.getTotalVisits())
            .memoryCount(memorial.getMemoryCount())
            .aiTrainingCompleted(memorial.getAiTrainingCompleted())
            .formattedAge(memorial.getFormattedAge())
            .relationshipDescription(memorial.getRelationship().getDisplayName())
            .canStartVideoCall(memorial.canStartVideoCall())
            .hasRequiredFiles(memorial.hasRequiredFiles())
            .hasRequiredProfileImages(hasRequiredProfileImages)
            .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
            .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
            .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))
            .isOwner(true)
            .canAccess(true)
            .build();
    }

    private MemorialListResponseDTO convertToListResponseDTOWithAccessInfo(Memorial memorial,
        boolean hasRequiredProfileImages, Member currentUser, boolean isOwner, FamilyMember familyMember) {

        MemorialListResponseDTO.MemorialListResponseDTOBuilder builder = MemorialListResponseDTO.builder()
            .memorialId(memorial.getId())
            .name(memorial.getName())
            .nickname(memorial.getNickname())
            .mainProfileImageUrl(memorial.getMainProfileImageUrl())
            .lastVisitAt(memorial.getLastVisitAt())
            .totalVisits(memorial.getTotalVisits())
            .memoryCount(memorial.getMemoryCount())
            .aiTrainingCompleted(memorial.getAiTrainingCompleted())
            .formattedAge(memorial.getFormattedAge())
            .relationshipDescription(memorial.getRelationship().getDisplayName())
            .canStartVideoCall(memorial.canStartVideoCall())
            .hasRequiredFiles(memorial.hasRequiredFiles())
            .hasRequiredProfileImages(hasRequiredProfileImages)
            .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
            .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
            .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))
            .isOwner(isOwner)
            .canAccess(true);

        if (isOwner) {
            builder.accessType("OWNER")
                .canModify(true)
                .canVideoCall(true);
        } else if (familyMember != null) {
            builder.accessType("FAMILY_MEMBER")
                .canModify(false)
                .canVideoCall(familyMember.getVideoCallAccess())
                .familyRelationship(familyMember.getRelationship().name())
                .familyRelationshipDisplay(familyMember.getRelationshipDisplayName());
        }

        return builder.build();
    }

    private void validateFileCount(List<MultipartFile> profileImages,
                                   List<MultipartFile> voiceFiles,
                                   MultipartFile videoFile) {

        if (profileImages == null || profileImages.size() != 5) {
            throw new IllegalArgumentException("프로필 이미지는 정확히 5장이 필요합니다.");
        }

        if (voiceFiles == null || voiceFiles.size() != 3) {
            throw new IllegalArgumentException("음성 파일은 정확히 3개가 필요합니다.");
        }

        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("영상 파일은 1개가 필요합니다.");
        }

        boolean hasEmptyProfileImage = profileImages.stream().anyMatch(MultipartFile::isEmpty);
        boolean hasEmptyVoiceFile = voiceFiles.stream().anyMatch(MultipartFile::isEmpty);

        if (hasEmptyProfileImage) {
            throw new IllegalArgumentException("업로드된 프로필 이미지 중 빈 파일이 있습니다.");
        }

        if (hasEmptyVoiceFile) {
            throw new IllegalArgumentException("업로드된 음성 파일 중 빈 파일이 있습니다.");
        }
    }

    private void validateQuestionAnswers(List<MemorialQuestionAnswerDTO> questionAnswers) {
        if (questionAnswers == null || questionAnswers.isEmpty()) {
            throw new IllegalArgumentException("질문 답변은 최소 1개 이상 필요합니다.");
        }

        log.info("질문 답변 유효성 검사 시작 - 답변 수: {}", questionAnswers.size());

        try {
            List<MemorialQuestion> activeQuestions = memorialQuestionRepository.findActiveQuestions();
            Map<Long, MemorialQuestion> questionMap = activeQuestions.stream()
                    .collect(Collectors.toMap(MemorialQuestion::getId, q -> q));

            for (MemorialQuestion question : activeQuestions) {
                if (question.getIsRequired()) {
                    boolean hasAnswer = questionAnswers.stream()
                            .anyMatch(dto -> dto.getQuestionId().equals(question.getId()) && dto.hasAnswer());

                    if (!hasAnswer) {
                        throw new IllegalArgumentException(
                                String.format("필수 질문에 답변이 없습니다: %s", question.getQuestionText()));
                    }
                }
            }

            for (MemorialQuestionAnswerDTO answerDTO : questionAnswers) {
                if (!answerDTO.hasAnswer()) {
                    continue;
                }

                MemorialQuestion question = questionMap.get(answerDTO.getQuestionId());
                if (question == null) {
                    throw new IllegalArgumentException(
                            String.format("존재하지 않는 질문입니다: %d", answerDTO.getQuestionId()));
                }

                int answerLength = answerDTO.getAnswerLength();
                if (answerLength < question.getMinLength()) {
                    throw new IllegalArgumentException(
                            String.format("답변이 너무 짧습니다. 최소 %d자 이상 입력해주세요: %s",
                                    question.getMinLength(), question.getQuestionText()));
                }

                if (answerLength > question.getMaxLength()) {
                    throw new IllegalArgumentException(
                            String.format("답변이 너무 깁니다. 최대 %d자 이하로 입력해주세요: %s",
                                    question.getMaxLength(), question.getQuestionText()));
                }
            }

            log.info("질문 답변 유효성 검사 완료 - 유효한 답변 수: {}",
                    questionAnswers.stream().filter(MemorialQuestionAnswerDTO::hasAnswer).count());

        } catch (Exception e) {
            log.error("질문 답변 유효성 검사 실패: {}", e.getMessage());
            throw new IllegalArgumentException("질문 답변 유효성 검사에 실패했습니다: " + e.getMessage());
        }
    }

    private MemorialListResponseDTO buildListResponseDTO(
        Memorial memorial,
        boolean hasRequiredProfileImages,
        Member currentUser,
        boolean isOwner,
        FamilyMember familyMember
) {
    // — 1) 전체 답변 필드 개수
    int totalAnswered = memorialAnswerRepository.countByMemorial(memorial);
    // — 2) 필수 질문 개수
    long requiredQuestionCount = memorialQuestionRepository.countByIsRequiredTrue();
    // — 3) 필수 질문 중 실제로 답변된 개수
    int answeredRequired = memorialAnswerRepository
        .countByMemorialAndQuestionIsRequiredTrue(memorial);

    return MemorialListResponseDTO.builder()
        .memorialId(memorial.getId())
        .name(memorial.getName())
        .nickname(memorial.getNickname())
        .mainProfileImageUrl(memorial.getMainProfileImageUrl())
        .lastVisitAt(memorial.getLastVisitAt())
        .totalVisits(memorial.getTotalVisits())
        .memoryCount(memorial.getMemoryCount())
        .aiTrainingCompleted(memorial.getAiTrainingCompleted())
        .formattedAge(memorial.getFormattedAge())
        .relationshipDescription(memorial.getRelationship().getDisplayName())
        .canStartVideoCall(memorial.canStartVideoCall())
        .hasRequiredProfileImages(hasRequiredProfileImages)
        .hasRequiredFiles(memorial.hasRequiredFiles())
        .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
        .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
        .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))

        // 접근 권한
        .isOwner(isOwner)
        .canAccess(true) // 이미 권한 체크 끝난 데이터니까
        .accessType(isOwner ? "OWNER" : "FAMILY_MEMBER")
        .canVideoCall(isOwner ? true : familyMember.getVideoCallAccess())
        .familyRelationship(isOwner ? null : familyMember.getRelationship().name())
        .familyRelationshipDisplay(isOwner ? null : familyMember.getRelationshipDisplayName())

        // —— 여기가 핵심: 고인 정보 관련 필드
        .deceasedInfoFieldCount(totalAnswered)
        .hasDeceasedInfo(totalAnswered > 0)
        .hasRequiredDeceasedInfo(answeredRequired >= requiredQuestionCount)

        .build();
}
}