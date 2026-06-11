package com.uniform.store.service;

import com.uniform.store.dto.request.AdminInviteRequest;
import com.uniform.store.dto.request.AdminUserFilterRequest;
import com.uniform.store.dto.response.AdminInviteResponse;
import com.uniform.store.dto.response.AdminUserDetailDto;
import com.uniform.store.dto.response.AdminUserSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    PageResponse<AdminUserSummaryDto> list(AdminUserFilterRequest filter, Pageable pageable);

    AdminUserDetailDto get(Long id);

    AdminInviteResponse invite(AdminInviteRequest req);

    AdminUserDetailDto suspend(Long id, String actingEmail);

    AdminUserDetailDto activate(Long id, String actingEmail);

    AdminUserDetailDto softDelete(Long id, String actingEmail);
}
