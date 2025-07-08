package com.tomato.naraclub.application.member.service;

import com.tomato.naraclub.application.member.code.ActivityReviewStage;
import com.tomato.naraclub.application.member.dto.MemberActivityRequest;
import com.tomato.naraclub.application.member.dto.MemberActivityResponse;
import com.tomato.naraclub.application.member.entity.Member;
import com.tomato.naraclub.application.member.entity.MemberActivity;
import com.tomato.naraclub.application.member.repository.MemberActivityRepository;
import com.tomato.naraclub.application.member.repository.MemberRepository;
import com.tomato.naraclub.application.security.MemberUserDetails;
import com.tomato.naraclub.common.code.MemberStatus;
import com.tomato.naraclub.common.code.ResponseStatus;
import com.tomato.naraclub.common.dto.ListDTO;
import com.tomato.naraclub.common.exception.APIException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.service
 * @fileName : MemberActivityServiceImpl
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberActivityServiceImpl implements MemberActivityService {

    private final MemberRepository memberRepository;
    private final MemberActivityRepository memberActivityRepository;

    @Override
    public ListDTO<MemberActivityResponse> getMemberActivities(MemberUserDetails userDetails,
        Pageable pageable) {

        Member member = memberRepository.findByIdAndStatus(userDetails.getMember().getId(), MemberStatus.ACTIVE)
            .orElseThrow(() -> new APIException(ResponseStatus.FORBIDDEN));

        return memberActivityRepository.getMemberActivities(member.getId() , pageable);
    }

    @Override
    @Transactional
    public MemberActivityResponse createMemberActivity(MemberUserDetails userDetails,
        MemberActivityRequest activity) {

        Member member = memberRepository.findByIdAndStatus(userDetails.getMember().getId(), MemberStatus.ACTIVE)
            .orElseThrow(() -> new APIException(ResponseStatus.FORBIDDEN));

        memberActivityRepository.findByAuthorAndShareLink(member, activity.getShareLink())
            .ifPresent(existing -> {throw new APIException(ResponseStatus.ALREADY_EXIST_URL);});

        MemberActivity saved = memberActivityRepository.save(MemberActivity.builder()
                .author(member)
                .title(activity.getTitle())
                .shareLink(activity.getShareLink())
                .stage(ActivityReviewStage.PENDING_REVIEW)
                .isDeleted(false)
            .build());

        return saved.convertDTO();
    }

    @Override
    @Transactional
    public MemberActivityResponse deleteMemberActivity(Long id, MemberUserDetails userDetails) {
        Member member = memberRepository.findByIdAndStatus(userDetails.getMember().getId(), MemberStatus.ACTIVE)
            .orElseThrow(() -> new APIException(ResponseStatus.FORBIDDEN));

        MemberActivity memberActivity = memberActivityRepository.findById(id)
            .orElseThrow(() -> new APIException(ResponseStatus.ARTICLE_NOT_EXIST));

        if(!Objects.equals(memberActivity.getAuthor().getId(), member.getId())) {
            throw new APIException(ResponseStatus.FORBIDDEN);
        }

        memberActivity.delete();

        return new MemberActivityResponse();
    }
}
