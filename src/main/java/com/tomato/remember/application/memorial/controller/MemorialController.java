package com.tomato.remember.application.memorial.controller;

import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialUpdateRequestDTO;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 메모리얼 뷰 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/mobile/memorial")
@RequiredArgsConstructor
public class MemorialController {
    
    private final MemorialService memorialService;
    
    /**
     * 메모리얼 생성 페이지
     */
    @GetMapping("/create")
    public String createMemorialPage(@AuthenticationPrincipal MemberUserDetails userDetails, 
                                   Model model) {
        log.info("Memorial create page access - User: {}", userDetails.getMember().getName());
        
        model.addAttribute("pageTitle", "새 메모리얼 등록 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails.getMember());
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("memorialCreateRequest", new MemorialCreateRequestDTO());
        
        return "/mobile/memorial/create";
    }

    /**
     * 메모리얼 생성 처리
     */
    @PostMapping("/create")
    public String createMemorial(@AuthenticationPrincipal MemberUserDetails userDetails,
                               @Valid @ModelAttribute("memorialCreateRequest") MemorialCreateRequestDTO createRequest,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               HttpServletRequest request) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial creation request from user: {} (ID: {}), memorial name: {}",
                currentUser.getName(), currentUser.getId(), createRequest.getName());

        // 유효성 검사 실패 시
        if (bindingResult.hasErrors()) {
            log.warn("Memorial creation validation failed for user: {} (ID: {})",
                    currentUser.getName(), currentUser.getId());

            model.addAttribute("pageTitle", "새 메모리얼 등록 - 토마토리멤버");
            model.addAttribute("appName", "토마토리멤버");
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isLoggedIn", true);

            return "/mobile/memorial/create";
        }

        try {
            // 메모리얼 생성
            MemorialResponseDTO createdMemorial = memorialService.createMemorial(createRequest, currentUser);

            log.info("Memorial created successfully: ID={}, Name={}, Owner={}",
                    createdMemorial.getId(), createdMemorial.getName(), currentUser.getName());

            redirectAttributes.addFlashAttribute("successMessage",
                    "메모리얼이 성공적으로 생성되었습니다.");

            return "redirect:/mobile/memorial/" + createdMemorial.getId();

        } catch (Exception e) {
            log.error("Memorial creation failed for user: {} (ID: {}), error: {}",
                    currentUser.getName(), currentUser.getId(), e.getMessage());

            model.addAttribute("pageTitle", "새 메모리얼 등록 - 토마토리멤버");
            model.addAttribute("appName", "토마토리멤버");
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("errorMessage", e.getMessage());

            return "/mobile/memorial/create";
        }
    }

    /**
     * 메모리얼 상세 페이지
     */
    @GetMapping("/{memorialId}")
    public String memorialDetailPage(@AuthenticationPrincipal MemberUserDetails userDetails,
                                   @PathVariable Long memorialId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        
        Member currentUser = userDetails.getMember();
        log.info("Memorial detail page access - User: {} (ID: {}), Memorial ID: {}", 
                currentUser.getName(), currentUser.getId(), memorialId);
        
        try {
            MemorialResponseDTO memorial = memorialService.getMemorialById(memorialId, currentUser);
            
            model.addAttribute("pageTitle", memorial.getName() + " - 토마토리멤버");
            model.addAttribute("appName", "토마토리멤버");
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("memorial", memorial);
            
            return "/mobile/memorial/detail";
            
        } catch (Exception e) {
            log.error("Memorial detail page access failed - User: {} (ID: {}), Memorial ID: {}, error: {}", 
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());
            
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/home";
        }
    }

    /**
     * 메모리얼 수정 페이지
     */
    @GetMapping("/{memorialId}/edit")
    public String editMemorialPage(@AuthenticationPrincipal MemberUserDetails userDetails,
                                 @PathVariable Long memorialId,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial edit page access - User: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        try {
            // 메모리얼 수정 폼 데이터 조회
            MemorialUpdateRequestDTO updateRequest = memorialService.getMemorialForUpdate(memorialId, currentUser);
            MemorialResponseDTO memorial = memorialService.getMemorialById(memorialId, currentUser);

            model.addAttribute("pageTitle", memorial.getName() + " 수정 - 토마토리멤버");
            model.addAttribute("appName", "토마토리멤버");
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("memorial", memorial);
            model.addAttribute("memorialUpdateRequest", updateRequest);

            return "/mobile/memorial/edit";

        } catch (Exception e) {
            log.error("Memorial edit page access failed - User: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/" + memorialId;
        }
    }

    /**
     * 메모리얼 수정 처리
     */
    @PostMapping("/{memorialId}/edit")
    public String updateMemorial(@AuthenticationPrincipal MemberUserDetails userDetails,
                               @PathVariable Long memorialId,
                               @Valid @ModelAttribute("memorialUpdateRequest") MemorialUpdateRequestDTO updateRequest,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial update request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        // 유효성 검사 실패 시
        if (bindingResult.hasErrors()) {
            log.warn("Memorial update validation failed for user: {} (ID: {}), Memorial ID: {}",
                    currentUser.getName(), currentUser.getId(), memorialId);

            try {
                MemorialResponseDTO memorial = memorialService.getMemorialById(memorialId, currentUser);
                model.addAttribute("pageTitle", memorial.getName() + " 수정 - 토마토리멤버");
                model.addAttribute("appName", "토마토리멤버");
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("isLoggedIn", true);
                model.addAttribute("memorial", memorial);

                return "/mobile/memorial/edit";

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/mobile/memorial/" + memorialId;
            }
        }

        try {
            // 메모리얼 수정
            MemorialResponseDTO updatedMemorial = memorialService.updateMemorial(memorialId, updateRequest, currentUser);

            log.info("Memorial updated successfully: ID={}, Name={}, Owner={}",
                    updatedMemorial.getId(), updatedMemorial.getName(), currentUser.getName());

            redirectAttributes.addFlashAttribute("successMessage",
                    "메모리얼이 성공적으로 수정되었습니다.");

            return "redirect:/mobile/memorial/" + memorialId;

        } catch (Exception e) {
            log.error("Memorial update failed for user: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/" + memorialId + "/edit";
        }
    }

    /**
     * 메모리얼 삭제 확인 페이지
     */
    @GetMapping("/{memorialId}/delete")
    public String deleteMemorialConfirmPage(@AuthenticationPrincipal MemberUserDetails userDetails,
                                          @PathVariable Long memorialId,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial delete confirm page access - User: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        try {
            MemorialResponseDTO memorial = memorialService.getMemorialById(memorialId, currentUser);

            // 소유자 확인
            if (!memorial.getOwnerId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "삭제 권한이 없습니다.");
                return "redirect:/mobile/memorial/" + memorialId;
            }

            model.addAttribute("pageTitle", memorial.getName() + " 삭제 확인 - 토마토리멤버");
            model.addAttribute("appName", "토마토리멤버");
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("memorial", memorial);

            return "/mobile/memorial/delete-confirm";

        } catch (Exception e) {
            log.error("Memorial delete confirm page access failed - User: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/" + memorialId;
        }
    }

    /**
     * 메모리얼 삭제 처리
     */
    @PostMapping("/{memorialId}/delete")
    public String deleteMemorial(@AuthenticationPrincipal MemberUserDetails userDetails,
                                @PathVariable Long memorialId,
                                RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial deletion request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        try {
            memorialService.deleteMemorial(memorialId, currentUser);

            log.info("Memorial deleted successfully: Memorial ID={}", memorialId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "메모리얼이 성공적으로 삭제되었습니다.");

            return "redirect:/mobile/memorial/manage";

        } catch (Exception e) {
            log.error("Memorial deletion failed for user: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/" + memorialId;
        }
    }

    /**
     * 메모리얼 상태 변경 처리
     */
    @PostMapping("/{memorialId}/toggle-status")
    public String toggleMemorialStatus(@AuthenticationPrincipal MemberUserDetails userDetails,
                                     @PathVariable Long memorialId,
                                     RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial status toggle request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        try {
            memorialService.toggleMemorialStatus(memorialId, currentUser);

            log.info("Memorial status toggled successfully: Memorial ID={}", memorialId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "메모리얼 상태가 성공적으로 변경되었습니다.");

            return "redirect:/mobile/memorial/manage";

        } catch (Exception e) {
            log.error("Memorial status toggle failed for user: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/manage";
        }
    }

    /**
     * 메모리얼 복구 처리
     */
    @PostMapping("/{memorialId}/restore")
    public String restoreMemorial(@AuthenticationPrincipal MemberUserDetails userDetails,
                                 @PathVariable Long memorialId,
                                 RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial restoration request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        try {
            memorialService.restoreMemorial(memorialId, currentUser);

            log.info("Memorial restored successfully: Memorial ID={}", memorialId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "메모리얼이 성공적으로 복구되었습니다.");

            return "redirect:/mobile/memorial/manage";

        } catch (Exception e) {
            log.error("Memorial restoration failed for user: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/manage";
        }
    }

    /**
     * 메모리얼 완전 삭제 확인 페이지
     */
    @GetMapping("/{memorialId}/permanent-delete")
    public String permanentDeleteMemorialConfirmPage(@AuthenticationPrincipal MemberUserDetails userDetails,
                                                    @PathVariable Long memorialId,
                                                    Model model,
                                                    RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial permanent delete confirm page access - User: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        try {
            // 삭제된 메모리얼만 완전 삭제 가능
            List<MemorialResponseDTO> deletedMemorials = memorialService.getDeletedMemorialListByMember(currentUser)
                    .stream()
                    .map(memorial -> memorial.toResponseDTO())
                    .toList();

            MemorialResponseDTO memorial = deletedMemorials.stream()
                    .filter(m -> m.getId().equals(memorialId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("삭제된 메모리얼을 찾을 수 없습니다."));

            model.addAttribute("pageTitle", memorial.getName() + " 완전 삭제 확인 - 토마토리멤버");
            model.addAttribute("appName", "토마토리멤버");
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("memorial", memorial);

            return "/mobile/memorial/permanent-delete-confirm";

        } catch (Exception e) {
            log.error("Memorial permanent delete confirm page access failed - User: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/manage";
        }
    }

    /**
     * 메모리얼 완전 삭제 처리
     */
    @PostMapping("/{memorialId}/permanent-delete")
    public String permanentDeleteMemorial(@AuthenticationPrincipal MemberUserDetails userDetails,
                                        @PathVariable Long memorialId,
                                        @RequestParam(required = false) String confirmText,
                                        RedirectAttributes redirectAttributes) {

        Member currentUser = userDetails.getMember();
        log.warn("Memorial permanent deletion request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        // 확인 텍스트 검증 (보안 강화)
        if (!"완전삭제".equals(confirmText)) {
            redirectAttributes.addFlashAttribute("errorMessage", "확인 텍스트가 올바르지 않습니다.");
            return "redirect:/mobile/memorial/" + memorialId + "/permanent-delete";
        }

        try {
            memorialService.permanentlyDeleteMemorial(memorialId, currentUser);

            log.warn("Memorial permanently deleted: Memorial ID={}", memorialId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "메모리얼이 완전히 삭제되었습니다. 이 작업은 복구할 수 없습니다.");

            return "redirect:/mobile/memorial/manage";

        } catch (Exception e) {
            log.error("Memorial permanent deletion failed for user: {} (ID: {}), Memorial ID: {}, error: {}",
                    currentUser.getName(), currentUser.getId(), memorialId, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mobile/memorial/manage";
        }
    }
}