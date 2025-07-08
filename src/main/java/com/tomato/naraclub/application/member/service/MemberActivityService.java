package com.tomato.naraclub.application.member.service;

import com.tomato.naraclub.application.member.dto.MemberActivityRequest;
import com.tomato.naraclub.application.member.dto.MemberActivityResponse;
import com.tomato.naraclub.application.security.MemberUserDetails;
import com.tomato.naraclub.common.dto.ListDTO;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.service
 * @fileName : MemberActivityService
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface MemberActivityService {

    ListDTO<MemberActivityResponse> getMemberActivities(MemberUserDetails userDetails, Pageable pageable);

    MemberActivityResponse createMemberActivity(MemberUserDetails userDetails, MemberActivityRequest activity);

    MemberActivityResponse deleteMemberActivity(Long id, MemberUserDetails userDetails);
}
