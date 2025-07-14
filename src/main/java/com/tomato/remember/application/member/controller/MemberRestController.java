package com.tomato.remember.application.member.controller;

import com.tomato.remember.application.member.dto.MemberInviteRequest;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.application.member.dto.MemberUpdateRequest;
import com.tomato.remember.application.member.service.MemberService;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberRestController {

    private final MemberService memberService;

    @PostMapping("/invite")
    public ResponseDTO<MemberDTO> enrollInviteCode(
        @RequestBody MemberInviteRequest request,
        @AuthenticationPrincipal MemberUserDetails userDetails) {
        return ResponseDTO.ok(memberService.enrollInviteCode(request.getInviteCode(), userDetails));
    }

    @PutMapping("/name")
    public ResponseDTO<MemberDTO> updateName(
        @RequestBody MemberUpdateRequest request,
        @AuthenticationPrincipal MemberUserDetails userDetails
    ){
        return ResponseDTO.ok(memberService.updateName(request,userDetails));
    }

    @PutMapping("/profile-image")
    public ResponseDTO<MemberDTO> updateProfileImage(
        @AuthenticationPrincipal MemberUserDetails userDetails,
        @RequestPart("file") MultipartFile file) {
        return ResponseDTO.ok(memberService.updateProfileImg(file, userDetails));
    }



}