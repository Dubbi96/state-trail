export type TRole =
    | 'ADMIN' //모든 농부 목록 조회
    | 'VILLAGE_HEAD' //본인에게 직접 소속된 농부 목록 조회
    | 'VICE_ADMIN_HEAD_OFFICER' //자신의 Area 내 VillageHead에 속한 농부 목록 조회
    | 'VICE_ADMIN_AGRICULTURE_MINISTRY_OFFICER'; //자신의 Area 내 VillageHead에 속한 농부 목록 조회
