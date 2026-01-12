import { TRequestMethod, TRequestServiceType, TRequestStatus } from '@/typings/Requests';

export const REQUEST_STATUS: Record<TRequestStatus, { label: string; color: string }> = {
    PENDING: {
        label: '대기',
        color: 'yellow',
    },
    APPROVED: {
        label: '승인',
        color: 'blue',
    },
    REJECTED: {
        label: '거절',
        color: 'red',
    },
};

export const REQUEST_SERVICE_TYPE: Record<TRequestServiceType, string> = {
    VILLAGE_HEAD: '면장',
    FARMER: '농부',
    PURCHASE: '수매',
    TREES_TRANSACTION: '나무 수령',
    SECTION: '지역',
};

export const REQUEST_METHOD: Record<TRequestMethod, string> = {
    CREATE: '등록',
    DELETE: '삭제',
    UPDATE: '수정',
};
