package com.authserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.authserver.entity.Ride;
import com.authserver.service.RideService;
import com.authserver.dto.queue.*;

import java.util.List;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
@Tag(name = "놀이기구 API", description = "놀이기구 관리 및 대기열 관련 API")
public class RideController {

    private static final Logger logger = LoggerFactory.getLogger(RideController.class);
    private final RideService rideService;

    /**
     * POST /api/rides - 놀이기구 생성
     */
    @Operation(summary = "놀이기구 생성", description = "새로운 놀이기구를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<Ride> createRide(
            @Parameter(description = "놀이기구 이름", required = true, example = "롤러코스터")
            @RequestParam String name,
            @Parameter(description = "탑승 시간 (분)", required = true, example = "5")
            @RequestParam Integer ridingTime,
            @Parameter(description = "운영 여부", required = true, example = "true")
            @RequestParam Boolean isActive,
            @Parameter(description = "전체 수용 인원", required = true, example = "100")
            @RequestParam Integer capacityTotal,
            @Parameter(description = "프리미엄 수용 인원", required = true, example = "30")
            @RequestParam Integer capacityPremium,
            @Parameter(description = "일반 수용 인원", required = true, example = "70")
            @RequestParam Integer capacityGeneral,
            @Parameter(description = "짧은 설명", example = "스릴 넘치는 롤러코스터")
            @RequestParam(required = false) String shortDescription,
            @Parameter(description = "상세 설명", example = "최고 속도 120km/h로 달리는...")
            @RequestParam(required = false) String longDescription,
            @Parameter(description = "사진 URL", example = "https://example.com/photo.jpg")
            @RequestParam(required = false) String photo,
            @Parameter(description = "운영 시간", example = "09:00-18:00")
            @RequestParam(required = false) String operatingTime) {
        try {
            Ride ride = rideService.createRide(name, ridingTime, isActive,
                    capacityTotal, capacityPremium, capacityGeneral,
                    shortDescription, longDescription, photo, operatingTime);
            return ResponseEntity.status(HttpStatus.CREATED).body(ride);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/rides/{rideId} - 특정 놀이기구 조회
     */
    @Operation(summary = "놀이기구 조회", description = "ID로 특정 놀이기구를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "놀이기구를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{rideId}")
    public ResponseEntity<Ride> getRide(
            @Parameter(description = "놀이기구 ID", required = true)
            @PathVariable Long rideId) {
        try {
            Ride ride = rideService.getRide(rideId);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/rides - 운영 중인 놀이기구 조회 (대기열 정보 포함)
     */
    @Operation(summary = "운영 중인 놀이기구 조회(몇분 남았고 몇명 대기중인지까지 확인 가능)", description = "현재 운영 중인 놀이기구와 대기열 정보를 함께 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<RideWithQueueInfoDto>> getActiveRides() {
        try {
            List<RideWithQueueInfoDto> result = rideService.getActiveRidesWithQueueInfo();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("운영 중인 놀이기구 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/rides/search - 이름으로 놀이기구 검색
     */
    @Operation(summary = "놀이기구 검색", description = "이름으로 놀이기구를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<List<Ride>> searchRides(
            @Parameter(description = "검색할 놀이기구 이름", required = true, example = "롤러")
            @RequestParam String name) {
        try {
            List<Ride> rides = rideService.searchRidesByName(name);
            return ResponseEntity.ok(rides);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/rides/{rideId} - 놀이기구 정보 수정
     */
    @Operation(summary = "놀이기구 수정", description = "놀이기구 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "놀이기구를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/{rideId}")
    public ResponseEntity<Ride> updateRide(
            @Parameter(description = "놀이기구 ID", required = true)
            @PathVariable Long rideId,
            @Parameter(description = "놀이기구 이름", example = "롤러코스터")
            @RequestParam(required = false) String name,
            @Parameter(description = "탑승 시간 (분)", example = "5")
            @RequestParam(required = false) Integer ridingTime,
            @Parameter(description = "운영 여부", example = "true")
            @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "전체 수용 인원", example = "100")
            @RequestParam(required = false) Integer capacityTotal,
            @Parameter(description = "프리미엄 수용 인원", example = "30")
            @RequestParam(required = false) Integer capacityPremium,
            @Parameter(description = "일반 수용 인원", example = "70")
            @RequestParam(required = false) Integer capacityGeneral,
            @Parameter(description = "짧은 설명", example = "스릴 넘치는 롤러코스터")
            @RequestParam(required = false) String shortDescription,
            @Parameter(description = "상세 설명", example = "최고 속도 120km/h로 달리는...")
            @RequestParam(required = false) String longDescription,
            @Parameter(description = "사진 URL", example = "https://example.com/photo.jpg")
            @RequestParam(required = false) String photo,
            @Parameter(description = "운영 시간", example = "09:00-18:00")
            @RequestParam(required = false) String operatingTime) {
        try {
            Ride ride = rideService.updateRide(rideId, name, ridingTime, isActive,
                    capacityTotal, capacityPremium, capacityGeneral,
                    shortDescription, longDescription, photo, operatingTime);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/rides/{rideId} - 놀이기구 삭제
     */
    @Operation(summary = "놀이기구 삭제", description = "놀이기구를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{rideId}")
    public ResponseEntity<Void> deleteRide(
            @Parameter(description = "놀이기구 ID", required = true)
            @PathVariable Long rideId) {
        try {
            rideService.deleteRide(rideId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
