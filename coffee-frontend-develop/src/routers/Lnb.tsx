import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router';

import { ExpandMore } from '@mui/icons-material';
import { Accordion, AccordionSummary, Stack, Typography } from '@mui/material';

import { getCookies } from '@/apis/AppUser/cookie';
import { palette } from '@/themes';
import { TRole } from '@/typings/User';

type TLnbChild = {
    label: string;
    path: string;
};

type TLnbItem = {
    label: string;
    children: TLnbChild[];
};

const lnbData: Record<TRole, TLnbItem[]> = {
    ADMIN: [
        {
            label: '요청 관리',
            children: [{ label: '요청 목록', path: '/requests' }],
        },
        {
            label: '면장 관리',
            children: [
                { label: '면장 목록', path: '/village-heads' },
                { label: '농부 목록', path: '/village-heads/farmers' },
                // { label: '농부 등록', path: '/village-heads/farmers/register' },
            ],
        },
        {
            label: '수매 관리',
            children: [{ label: '수매 목록', path: '/trees-purchases' }],
        },
        {
            label: '부관리자 관리',
            children: [{ label: '부관리자 목록', path: '/vice-admins' }],
        },
        {
            label: '지역 관리',
            children: [
                { label: '지역 및 섹션 목록', path: '/locations' },
                { label: '지역 생성', path: '/locations/register' },
                { label: '섹션 생성', path: '/locations/register/section' },
            ],
        },
        // {
        //     label: '계정 관리',
        //     children: [{ label: '계정 생성', path: '/accounts/register' }],
        // },
    ],
    // 부관리자(농림부)
    VICE_ADMIN_AGRICULTURE_MINISTRY_OFFICER: [
        {
            label: '요청 관리',
            children: [{ label: '요청 목록', path: '/requests' }],
        },
        {
            label: '면장 관리',
            children: [
                { label: '면장 목록', path: '/village-heads' },
                { label: '농부 목록', path: '/village-heads/farmers' },
                // { label: '농부 등록', path: '/village-heads/farmers/register' },
            ],
        },
        // {
        //     label: '수매 관리',
        //     children: [{ label: '수매 목록', path: '/trees-purchases' }],
        // },
        {
            label: '지역 관리',
            children: [
                { label: '지역 및 섹션 목록', path: '/locations' },
                { label: '섹션 생성', path: '/locations/register/section' },
            ],
        },
    ],
    // 부관리자(한국지사)
    VICE_ADMIN_HEAD_OFFICER: [
        {
            label: '요청 관리',
            children: [{ label: '요청 목록', path: '/requests' }],
        },
        {
            label: '면장 관리',
            children: [
                { label: '면장 목록', path: '/village-heads' },
                { label: '농부 목록', path: '/village-heads/farmers' },
                // { label: '농부 등록', path: '/village-heads/farmers/register' },
            ],
        },
        {
            label: '수매 관리',
            children: [{ label: '수매 목록', path: '/trees-purchases' }],
        },
        {
            label: '지역 관리',
            children: [
                { label: '지역 및 섹션 목록', path: '/locations' },
                { label: '섹션 생성', path: '/locations/register/section' },
            ],
        },
    ],
    VILLAGE_HEAD: [
        {
            label: '면장 관리',
            children: [
                { label: '농부 목록', path: '/village-heads/farmers' },
                { label: '수매 내역', path: '/trees-purchases' },
            ],
        },
    ],
};

export const LNB_WIDTH = 250;

const Lnb = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { pathname } = useLocation();

    const role = getCookies('role') as TRole;

    return (
        <Stack
            sx={{
                padding: '12px',
                minWidth: LNB_WIDTH,
                gap: '5px',
                borderRight: `1px solid ${palette.grey[50]}`,
                flex: 1,
            }}
        >
            {lnbData[role].map((v) => {
                return (
                    <Accordion
                        key={v.label}
                        sx={{
                            'boxShadow': 'none',
                            '&::before': {
                                content: 'none',
                            },
                            '&.Mui-expanded': { margin: 0 },
                        }}
                        defaultExpanded
                    >
                        <AccordionSummary
                            expandIcon={<ExpandMore sx={{ color: palette.grey[500] }} />}
                            sx={{
                                'backgroundColor': '#F9FAFB',
                                'padding': '12px',
                                'borderRadius': '10px',
                                'height': '48px',
                                'fontSize': '15px',
                                'color': palette.grey[800],
                                '&.Mui-expanded': { minHeight: '48px', marginBottom: '5px' },
                            }}
                        >
                            <Typography sx={{ color: palette.grey[800], fontSize: '15px' }}>
                                {t(v.label)}
                            </Typography>
                        </AccordionSummary>
                        {v.children && (
                            <Stack
                                sx={{
                                    gap: '4px',
                                    margin: '0px',
                                }}
                            >
                                {v.children.map((c) => {
                                    const selected = pathname === c.path;
                                    return (
                                        <Stack
                                            key={c.path}
                                            onClick={() => navigate(c.path)}
                                            sx={{
                                                'borderRadius': '10px',
                                                'height': '48px',
                                                'backgroundColor': selected
                                                    ? palette.blue[50]
                                                    : palette.common.white,
                                                'padding': '0 0 0 20px',
                                                'flexDirection': 'row',
                                                'alignItems': 'center',
                                                'cursor': 'pointer',
                                                '&:hover': {
                                                    backgroundColor: palette.grey[50],
                                                },
                                            }}
                                        >
                                            <Typography
                                                sx={{
                                                    fontSize: '15px',
                                                    color: selected
                                                        ? palette.blue[600]
                                                        : palette.grey[700],
                                                    fontWeight: selected ? 600 : 400,
                                                }}
                                            >
                                                {t(c.label)}
                                            </Typography>
                                        </Stack>
                                    );
                                })}
                            </Stack>
                        )}
                    </Accordion>
                );
            })}
        </Stack>
    );
};

export default Lnb;
