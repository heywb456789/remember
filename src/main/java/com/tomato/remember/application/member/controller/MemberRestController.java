package com.tomato.remember.application.member.controller;

import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.dto.MemberInviteRequest;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.application.member.dto.MemberUpdateRequest;
import com.tomato.remember.application.member.dto.MyInfoDTO;
import com.tomato.remember.application.member.dto.ProfileSettingsDTO;
import com.tomato.remember.application.member.dto.RelationshipDTO;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.service.MemberProfileService;
import com.tomato.remember.application.member.service.MemberService;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberRestController {

    private final MemberService memberService;
    private final MemberProfileService memberProfileService;

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

    @GetMapping("/my-info")
    public ResponseDTO<MyInfoDTO> getMyInfo(
        @AuthenticationPrincipal MemberUserDetails userDetails
    ){
        Member currentUser = userDetails ==null ? null : userDetails.getMember();
        if(currentUser == null){
            throw new APIException(ResponseStatus.USER_NOT_EXIST);
        }
        ProfileSettingsDTO profileData = memberProfileService.getProfileSettingsData(currentUser.getId());

        return ResponseDTO.ok(
            MyInfoDTO.builder()
                .member(currentUser.convertDTO())
                .profile(profileData)
                .build()
        );
    }

    @GetMapping("/relationship")
    public ResponseDTO<List<RelationshipDTO>> getAllRelationShip(){
        List<RelationshipDTO> list = Arrays.stream(Relationship.values())
            .map(r -> new RelationshipDTO(r.name(), r.getDisplayName()))
            .collect(Collectors.toList());
        return ResponseDTO.ok(list);
    }

}