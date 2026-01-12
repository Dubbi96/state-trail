import { useState } from 'react';

import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { AccountCircle } from '@mui/icons-material';
import { MenuItem, Select, Stack, Typography } from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';

import Ko from '@assets/ko.png';
import Lo from '@assets/lo.png';
import Logo from '@assets/logo.png';

import { getCookies, removeCookies } from '@/apis/AppUser/cookie';
import { palette } from '@/themes';

const GNB_HEIGHT = 60;
type GnbProps = {
    isLoginPage: boolean;
};

const Gnb = ({ isLoginPage }: GnbProps) => {
    const { t, i18n } = useTranslation();
    const isLogin = true;
    const navigate = useNavigate();
    const userId = getCookies('userId');
    const queryClient = useQueryClient();

    const handleClickLogout = () => {
        removeCookies('accessToken');
        removeCookies('role');
        removeCookies('appUserId');
        removeCookies('userId');

        queryClient.resetQueries();
        navigate('/login');
    };

    const [lang, setLang] = useState(i18n.language);
    const handleChangeLang = (e: any) => {
        setLang(e.target.value);
        i18n.changeLanguage(e.target.value);
    };

    const isVillageHead = getCookies('role') === 'VILLAGE_HEAD' ? true : false;

    return (
        <Stack
            sx={{
                '& .MuiTypography-root': {
                    color: palette.common.white,
                },
                'backgroundColor': palette.blue[500],
                'padding': '0 20px 0 15px',
                'flexDirection': 'row',
                'justifyContent': 'space-between',
                'alignItems': 'center',
                'height': GNB_HEIGHT,
                'minHeight': GNB_HEIGHT,
            }}
        >
            <Stack
                sx={{
                    height: '40px',
                    width: '75px',
                    backgroundImage: `url(${Logo})`,
                    backgroundSize: 'contain',
                    backgroundRepeat: 'no-repeat',
                    backgroundPosition: 'center',
                    cursor: 'pointer',
                }}
                onClick={() => navigate(isVillageHead ? '/village-heads/farmers' : '/requests')}
            />
            <Stack direction={'row'} gap={2}>
                {isLogin && !isLoginPage && (
                    <Stack sx={{ flexDirection: 'row', gap: '15px', alignItems: 'center' }}>
                        <Stack sx={{ flexDirection: 'row', gap: '6px', alignItems: 'center' }}>
                            <AccountCircle sx={{ color: '#FFFFFF' }} />
                            <Typography flexShrink={0}>{userId}</Typography>
                        </Stack>
                        <Typography
                            flexShrink={0}
                            sx={{ cursor: 'pointer' }}
                            onClick={() => navigate('/my-profile-edit')}
                        >
                            {t('내 정보 수정')}
                        </Typography>
                        <Typography
                            sx={{ cursor: 'pointer', flexShrink: 0 }}
                            onClick={handleClickLogout}
                        >
                            {t('로그아웃')}
                        </Typography>
                    </Stack>
                )}
                <Stack width={'150px'}>
                    <Select size="small" value={lang} onChange={handleChangeLang} fullWidth>
                        <MenuItem value={'ko'}>
                            <Stack direction={'row'} gap={1}>
                                <Stack
                                    sx={{
                                        width: '20px',
                                        height: '20px',
                                        backgroundImage: `url(${Ko})`,
                                        backgroundSize: 'contain',
                                        backgroundRepeat: 'no-repeat',
                                        backgroundPosition: 'center',
                                    }}
                                />
                                한국어
                            </Stack>
                        </MenuItem>
                        <MenuItem value={'lo'}>
                            <Stack direction={'row'} gap={1}>
                                <Stack
                                    sx={{
                                        height: '20px',
                                        width: '20px',
                                        backgroundImage: `url(${Lo})`,
                                        backgroundSize: 'contain',
                                        backgroundRepeat: 'no-repeat',
                                        backgroundPosition: 'center',
                                    }}
                                />
                                {t('라오스어')}
                            </Stack>
                        </MenuItem>
                    </Select>
                </Stack>
            </Stack>
        </Stack>
    );
};

export default Gnb;
