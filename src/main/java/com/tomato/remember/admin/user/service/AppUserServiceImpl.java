package com.tomato.remember.admin.user.service;

import com.tomato.remember.admin.security.AdminUserDetails;
import com.tomato.remember.admin.user.dto.ActivityResponse;
import com.tomato.remember.admin.user.dto.AppUserListRequest;
import com.tomato.remember.admin.user.dto.AppUserResponse;
import com.tomato.remember.admin.user.dto.UserActivityListResponse;
import com.tomato.remember.admin.user.dto.UserUpdateRequest;
import com.tomato.remember.admin.user.entity.AuthorityHistory;
import com.tomato.remember.admin.user.repository.AppUserLoginHistoryRepository;
import com.tomato.remember.admin.user.repository.AppUserRepository;
import com.tomato.remember.admin.user.repository.AuthorityHistoryRepository;
import com.tomato.remember.application.auth.entity.MemberLoginHistory;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.exception.APIException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.service
 * @fileName : AppUserServiceImpl
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private static final String ADMIN_BOARD = "/admin/board/";
    private static final String ADMIN_VOTE = "/admin/vote/";
    private static final String ADMIN_ORIGINAL_NEWS = "/admin/original/news";
    private static final String ADMIN_ORIGINAL_VIDEO = "/admin/original/video";
    private final AppUserRepository appUserRepository;
    private final AuthorityHistoryRepository authorityHistoryRepository;
    private final AppUserLoginHistoryRepository historyRepository;

    @Override
    public ListDTO<AppUserResponse> getAppUserList(AdminUserDetails user,
        AppUserListRequest request, Pageable pageable) {
        return appUserRepository.getAppUserList(user, request, pageable);
    }

    @Override
    @Transactional
    public AppUserResponse updateUserVerified(Long id, AdminUserDetails userDetails,
        UserUpdateRequest request) {

        Member member = appUserRepository.findById(id)
            .orElseThrow(()->new APIException(ResponseStatus.USER_NOT_EXIST));

//        if(member.getStatus().equals(request.getStatus())){
//            throw new APIException(ResponseStatus.ALREADY_MODIFIED_STATUS);
//        }
//        member.setStatus(request.getStatus());
//
//        if(request.getStatus().equals(MemberStatus.ACTIVE)){
//            member.setRole(MemberRole.USER_ACTIVE);
//        }else{
//            member.setRole(MemberRole.USER_INACTIVE);
//        }

        AuthorityHistory authorityHistory = authorityHistoryRepository.save(
            AuthorityHistory.builder()
                .userId(member.getId())
                .memberStatus(request.getStatus())
                .reason(request.getReason())
                .createdBy(userDetails.getAdmin().getId())
                .updatedBy(userDetails.getAdmin().getId())
                .build()
        );

        log.debug(authorityHistory.toString());

        return member.convertAppUserResponse();
    }

    @Override
    public AppUserResponse getAppUserDetail(long id) {
        Member member = appUserRepository.findById(id)
            .orElseThrow(()->new APIException(ResponseStatus.USER_NOT_EXIST));

        return member.convertAppUserResponse();
    }

    @Override
    public Page<MemberLoginHistory> getAppUserLoginHistory(long id, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return historyRepository.findByMemberId(id, pageable);
    }

    @Override
    public UserActivityListResponse getUserActivities(Long id, int page, int size, String type) {
        List<ActivityResponse> all = new ArrayList<>();


        // 5) 타입 필터링
        Stream<ActivityResponse> stream = all.stream();
        if (type != null && !type.isBlank()) {
            stream = stream.filter(a -> a.getType().equals(type));
        }

        // 6) 최신순 정렬
        List<ActivityResponse> sorted = stream
            .sorted(Comparator.comparing(ActivityResponse::getCreatedAt).reversed())
            .collect(Collectors.toList());

        // 7) 페이징 처리
        int from = page * size;
        int to   = Math.min(from + size, sorted.size());
        List<ActivityResponse> pageList =
            (from < sorted.size() ? sorted.subList(from, to) : List.of());
        boolean hasMore = sorted.size() > to;

        return new UserActivityListResponse(pageList, hasMore);
    }
}
