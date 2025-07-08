package com.tomato.naraclub.application.member.service;

import com.tomato.naraclub.application.member.dto.MemberDTO;
import com.tomato.naraclub.application.member.dto.MemberUpdateRequest;
import com.tomato.naraclub.application.security.MemberUserDetails;
import org.springframework.web.multipart.MultipartFile;

public interface MemberService {


    MemberDTO enrollInviteCode(String inviteCode, MemberUserDetails userDetails);

    MemberDTO updateName(MemberUpdateRequest request, MemberUserDetails userDetails);

    MemberDTO updateProfileImg(MultipartFile file, MemberUserDetails userDetails);
}
