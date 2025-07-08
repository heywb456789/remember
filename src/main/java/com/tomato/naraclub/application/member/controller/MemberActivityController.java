package com.tomato.naraclub.application.member.controller;

import com.tomato.naraclub.application.member.dto.MemberActivityRequest;
import com.tomato.naraclub.application.member.dto.MemberActivityResponse;
import com.tomato.naraclub.application.member.service.MemberActivityService;
import com.tomato.naraclub.application.security.MemberUserDetails;
import com.tomato.naraclub.common.dto.ListDTO;
import com.tomato.naraclub.common.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.controller
 * @fileName : MemberActivityController
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberActivityController {

    private final MemberActivityService memberActivityService;

    @GetMapping("/activities")
    public ResponseDTO<ListDTO<MemberActivityResponse>> getMemberActivities(
        @AuthenticationPrincipal MemberUserDetails userDetails,
        @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityId"));
        return ResponseDTO.ok(memberActivityService.getMemberActivities(userDetails, pageable));
    }

    @PostMapping("/activities")
    public ResponseDTO<MemberActivityResponse> createMemberActivity(
        @AuthenticationPrincipal MemberUserDetails userDetails,
        @RequestBody MemberActivityRequest activity){
        return ResponseDTO.ok(memberActivityService.createMemberActivity(userDetails, activity));
    }

    @DeleteMapping("/activities/{id}")
    public ResponseDTO<MemberActivityResponse> deleteMemberActivity(
        @PathVariable Long id,
        @AuthenticationPrincipal MemberUserDetails userDetails){
        return ResponseDTO.ok(memberActivityService.deleteMemberActivity(id, userDetails));
    }
}
