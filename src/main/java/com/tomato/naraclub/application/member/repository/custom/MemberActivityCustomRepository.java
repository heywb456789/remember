package com.tomato.naraclub.application.member.repository.custom;

import com.tomato.naraclub.application.member.dto.MemberActivityResponse;
import com.tomato.naraclub.common.dto.ListDTO;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.repository.custom
 * @fileName : MemberActivityCustomRepository
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface MemberActivityCustomRepository {

    ListDTO<MemberActivityResponse> getMemberActivities(Long id, Pageable pageable);
}
