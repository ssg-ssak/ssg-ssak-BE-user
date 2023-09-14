package ssgssak.ssgpointuser.domain.store.application;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ssgssak.ssgpointuser.domain.franchise.entity.Franchise;
import ssgssak.ssgpointuser.domain.franchise.entity.QFranchise;
import ssgssak.ssgpointuser.domain.store.dto.*;
import ssgssak.ssgpointuser.domain.store.entity.FavoriteStore;
import ssgssak.ssgpointuser.domain.store.entity.QStore;
import ssgssak.ssgpointuser.domain.store.entity.Store;
import ssgssak.ssgpointuser.domain.store.infrastructure.FavoriteStoreRepository;
import ssgssak.ssgpointuser.domain.store.infrastructure.StoreRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StoreServiceImpl implements StoreService{
    private final StoreRepository storeRepository;
    private final FavoriteStoreRepository favoriteStoreRepository;
    private final ModelMapper modelMapper;
    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 제휴 매장 & 단골매장
     * 1. 매장 지도로 검색하기
     * 2. 매장 지역으로 검색하기
     * 3. 단골매장 등록하기
     * 4. id로 매장 조회
     */

    // 1. 매장 지도로 검색하기 : 지도에 표시되는 위-경도의 경곗값을 전달받아서, 그 사이에 존재하는 매장만 넘겨줌
    @Override
    @Transactional(readOnly = true)
    public StoreGetMapResponseDto getByMap(StoreGetMapRequestDto requestDto) {
        List<Store> storeList = storeRepository.findAllByLatitudeBetweenAndLongitudeBetween(
                requestDto.getUpLatitude(),
                requestDto.getDownLatitude(),
                requestDto.getLeftLongitude(),
                requestDto.getRightLongitude());
        List mapDtos = new ArrayList<>();
        mapDtos.add("rows : " + storeList.size());
        for (Store s : storeList) {
            GetMapDto dto = modelMapper.map(s, GetMapDto.class);
            mapDtos.add(dto);
        }
        return StoreGetMapResponseDto.builder().mapDtos(mapDtos).build();
    }

    // 2. 매장 지역으로 검색하기 : 제휴사,시,군(구)를 넘겨받아서, 그 사이에 존재하는 매장만 넘겨줌
    @Override
    @Transactional(readOnly = true)
    public StoreGetRegionResponseDto getByRegion(StoreGetRegionRequestDto requestDto) {
        log.info("result"+requestDto);
        QStore store = QStore.store;
        QFranchise franchise = QFranchise.franchise;
        List<Store> storeByRegion = jpaQueryFactory
                .selectFrom(store)
                .join(store.franchise, franchise)
                .where(
                        eqFName(requestDto.getFranchiseName())
                                .and(eqCity(requestDto.getCity()))
                                .and(eqDistrict(requestDto.getDistrict()))
                )
                .fetch();

        // Lazy 직렬화 에러를 피하기 위해, franchise를 dto로 전환하여 리턴해준다
        List regionDtos = new ArrayList<>();
        regionDtos.add("rows : " + storeByRegion.size());
        for (Store s : storeByRegion) {
            Franchise f = s.getFranchise();
            GetFranchiseDto franchiseDto = GetFranchiseDto.builder()
                    .name(f.getName())
                    .logoUrl(f.getLogoUrl())
                    .homepageUrl(f.getHomepageUrl())
                    .build();
            GetRegionDto regionDto = modelMapper.map(s, GetRegionDto.class);
            regionDto = regionDto.toBuilder().franchiseDto(franchiseDto).build();
            regionDtos.add(regionDto);
        }
        return StoreGetRegionResponseDto.builder().regionDtos(regionDtos).build();
    }


    // 3. 단골매장 등록하기 : 매장 id값과 uuid를 넘겨받아서 진행, store_id가 아닌 store 전체를 저장하는것임
    @Override
    public void registerFavorite(StoreRegisterFavoriteRequestDto requestDto, String uuid) {
        Store store = storeRepository.findById(requestDto.getStoreId())
                .orElseThrow(()-> new NoSuchElementException());
        FavoriteStore favoriteStore = FavoriteStore.builder()
                .store(store)
                .userUUID(uuid)
                .build();
        favoriteStoreRepository.save(favoriteStore);
    }


    // 4. id로 매장 조회
    @Override
    public GetStoreDto getById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NoSuchElementException());
        GetStoreDto storeDto = modelMapper.map(store, GetStoreDto.class);
        storeDto.toBuilder().franchise_id(store.getFranchise().getId());
        return storeDto;
    }


    // BooleanExpression 설정
    private BooleanExpression eqFName(String name) {
        if (StringUtils.isEmpty(name)) {
            return QFranchise.franchise.name.isNotNull();
        }
        return QFranchise.franchise.name.eq(name);
    }

    private BooleanExpression eqCity(String city) {
        if (StringUtils.isEmpty(city)) {
            return QStore.store.city.isNotNull();
        }
        return QStore.store.city.eq(city);
    }

    private BooleanExpression eqDistrict(String district) {
        if (StringUtils.isEmpty(district)) {
            return QStore.store.district.isNotNull();
        }
        return QStore.store.district.eq(district);
    }

}
