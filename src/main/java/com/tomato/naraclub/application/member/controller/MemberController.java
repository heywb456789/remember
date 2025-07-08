package com.tomato.naraclub.application.member.controller;

import com.tomato.naraclub.application.member.dto.MemberInviteRequest;
import com.tomato.naraclub.application.member.dto.MemberDTO;
import com.tomato.naraclub.application.member.dto.MemberUpdateRequest;
import com.tomato.naraclub.application.member.service.MemberService;
import com.tomato.naraclub.application.oneld.service.TomatoAuthService;
import com.tomato.naraclub.application.security.MemberUserDetails;
import com.tomato.naraclub.common.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

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