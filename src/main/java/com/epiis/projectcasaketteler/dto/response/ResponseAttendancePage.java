package com.epiis.projectcasaketteler.dto.response;

import com.epiis.projectcasaketteler.entity.EntityAttendance;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.stream.Collectors;

public class ResponseAttendancePage {
    private List<ResponseAttendanceDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public ResponseAttendancePage(Page<EntityAttendance> page) {
        this.content = page.getContent().stream()
                .map(ResponseAttendanceDTO::new)
                .collect(Collectors.toList());
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
    }

    public List<ResponseAttendanceDTO> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }
}