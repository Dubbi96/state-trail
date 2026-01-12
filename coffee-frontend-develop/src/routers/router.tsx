import { createBrowserRouter } from 'react-router';

import { Stack } from '@mui/material';

import AccountRegister from '@/pages/Accounts/AccountRegister';
import Login from '@/pages/Accounts/Login';
import MyProfileEdit from '@/pages/Accounts/MyProfileEdit';
import FarmerList from '@/pages/Farmers';
import { AreaDetails } from '@/pages/Locations/LocationDetails/area';
import { SectionDetails } from '@/pages/Locations/LocationDetails/section';
import LocationList from '@/pages/Locations/LocationList';
import LocationRegister from '@/pages/Locations/LocationRegister';
import SectionRegister from '@/pages/Locations/SectionRegister';
import FarmerRequestDetails from '@/pages/Requests/FarmerRequestDetails';
import RequestList from '@/pages/Requests/RequestList';
import SectionRequestDetails from '@/pages/Requests/SectionRequestDetails';
import TreesPurchaseRequestDetails from '@/pages/Requests/TreesPurchaseRequestDetails';
import VillageHeadRequestDetails from '@/pages/Requests/VillageHeadRequestDetails';
import TestComponents from '@/pages/TestComponents';
import TreesPurchaseList from '@/pages/TreesPurchases';
import ViceAdminList from '@/pages/ViceAdmins';
import ViceAdminDetails from '@/pages/ViceAdmins/VIceAdminDetails';
import ViceAdminEdit from '@/pages/ViceAdmins/ViceAdminEdit';
import VillageHeadDetails from '@/pages/VillageHeads/VillageHeadDetails';
import VillageHeadList from '@/pages/VillageHeads/VillageHeadList';

import Gnb from './Gnb';
import Layouts from './Layouts';

const router = createBrowserRouter([
    {
        path: 'login',
        element: (
            <Stack sx={{ height: '100vh', width: '100vw' }}>
                <Gnb isLoginPage={true} />
                <Login />
            </Stack>
        ),
    },
    {
        path: '/',
        element: <Layouts />,
        children: [
            {
                path: 'my-profile-edit', // 요청
                element: <MyProfileEdit />,
            },
            {
                path: 'requests', // 요청
                children: [
                    { index: true, element: <RequestList /> },
                    { path: 'village_head/:id', element: <VillageHeadRequestDetails /> }, // 면장 등록 요청 상세
                    { path: 'farmer/:id', element: <FarmerRequestDetails /> }, // 농부 등록 요청 상세
                    { path: 'purchase/:id', element: <TreesPurchaseRequestDetails /> }, // 수매 승인 요청 상세
                    { path: 'section/:id', element: <SectionRequestDetails /> }, // 섹션 생성 요청 상세
                ],
            },
            {
                path: 'village-heads', // 면장
                children: [
                    { index: true, element: <VillageHeadList /> },
                    { path: ':id', element: <VillageHeadDetails /> },
                    { path: 'farmers', element: <FarmerList /> },
                ],
            },
            {
                path: 'trees-purchases', // 수매
                children: [{ index: true, element: <TreesPurchaseList /> }],
            },
            {
                path: 'vice-admins', // 부 관리자
                children: [
                    { index: true, element: <ViceAdminList /> },
                    { path: 'edit/:id', element: <ViceAdminEdit /> },
                    { path: ':id', element: <ViceAdminDetails /> },
                ],
            },
            {
                path: 'locations', // 지역
                children: [
                    { index: true, element: <LocationList /> },
                    { path: 'area/:id', element: <AreaDetails /> },
                    { path: 'section/:id', element: <SectionDetails /> },
                    { path: 'register', element: <LocationRegister /> },
                    { path: 'register/section', element: <SectionRegister /> },
                ],
            },
            {
                path: 'accounts', // 계정
                children: [{ path: 'register', element: <AccountRegister /> }],
            },
            {
                path: 'components', // 공통 컴포넌트
                element: <TestComponents />,
            },
        ],
    },
]);

export default router;
