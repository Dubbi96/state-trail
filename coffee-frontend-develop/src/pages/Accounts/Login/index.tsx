import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { Button, CircularProgress, Stack, TextField, Typography } from '@mui/material';

import { setCookies } from '@/apis/AppUser/cookie';
import { useSignIn } from '@/apis/AppUser/useSignIn';
import { TRole } from '@/typings/User';
import { showToast } from '@/utils/showToast';

export type TLoginForm = {
    userId: string;
    password: string;
};

const Login = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { mutateAsync: signIn, isPending } = useSignIn();
    const { register, handleSubmit } = useForm<TLoginForm>({
        defaultValues: {
            userId: '',
            password: '',
        },
    });

    const roleRouteMap: Record<TRole, string> = {
        ADMIN: '/requests',
        VICE_ADMIN_HEAD_OFFICER: '/requests',
        VICE_ADMIN_AGRICULTURE_MINISTRY_OFFICER: '/requests',
        VILLAGE_HEAD: '/village-heads/farmers',
    };

    const onSubmit = async (formData: TLoginForm) => {
        await signIn(formData).then((res) => {
            if (res.code === 'SUCCESS') {
                const { accessToken, role, appUserId, userId } = res.response;
                setCookies('accessToken', accessToken);
                setCookies('role', role);
                setCookies('appUserId', appUserId);
                setCookies('userId', userId);

                navigate(roleRouteMap[res.response.role as TRole]);
            } else {
                showToast.error(res.message);
            }
        });
    };

    return (
        <Stack
            sx={{
                width: '100%',
                height: '100%',
                alignItems: 'center',
                justifyContent: 'center',
            }}
            component="form"
            onSubmit={handleSubmit(onSubmit)}
        >
            <Stack
                sx={{
                    'gap': '30px',
                    'alignItems': 'center',
                    'width': '500px',
                    '& .MuiFormControl-root, .MuiButton-root': { width: '60%' },
                }}
            >
                <Typography variant="h1/bold">{t('로그인')}</Typography>
                {isPending ? (
                    <Stack alignContent={'center'} justifyContent={'center'} height={'195px'}>
                        <CircularProgress />
                    </Stack>
                ) : (
                    <Stack width={'100%'} alignItems={'center'} gap="30px">
                        <TextField placeholder={t('아이디')} {...register('userId')} />
                        <TextField
                            placeholder={t('비밀번호')}
                            {...register('password')}
                            type="password"
                        />
                        <Button variant="containedBlue" type="submit">
                            {t('로그인')}
                        </Button>
                    </Stack>
                )}
            </Stack>
        </Stack>
    );
};

export default Login;
