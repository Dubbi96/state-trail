import { Navigate, Outlet } from 'react-router';

import { Stack } from '@mui/material';

import { getCookies } from '@/apis/AppUser/cookie';

import Gnb from './Gnb';
import Lnb from './Lnb';

const Layouts = () => {
    const isLogin = getCookies('accessToken');

    if (!isLogin) return <Navigate to="/login" replace />;

    return (
        <Stack sx={{ height: '100%', maxHeight: '100vh', width: '100%', maxWidth: '100vw' }}>
            <Gnb isLoginPage={false} />
            <Stack
                sx={{
                    flexDirection: 'row',
                    width: '100%',
                    overflow: 'hidden',
                }}
            >
                <Stack sx={{ 'overflowY': 'auto', '&::-webkit-scrollbar': { display: 'none' } }}>
                    <Lnb />
                </Stack>
                <Stack
                    sx={{
                        flex: 1,
                        overflowY: 'auto',
                        paddingBottom: '32px',
                    }}
                >
                    <Outlet />
                </Stack>
            </Stack>
        </Stack>
    );
};

export default Layouts;
