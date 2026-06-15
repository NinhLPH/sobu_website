package com.vn.sodu.request;

import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class RequestEntityTest {

    @Test
    void requestDefaultsToPendingAndHasEmptyItemList() {
        Request request = Request.builder().build();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getItems()).isNotNull();
        assertThat(request.getItems()).isEmpty();
        assertThat(request.getAttachments()).isNotNull();
        assertThat(request.getAttachments()).isEmpty();
    }

    @Test
    void requestHasVersionFieldForOptimisticLocking() throws Exception {
        Field versionField = Request.class.getDeclaredField("version");

        assertThat(versionField.isAnnotationPresent(Version.class)).isTrue();
    }
}
