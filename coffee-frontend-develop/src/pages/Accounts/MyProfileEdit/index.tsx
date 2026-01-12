import { useEffect } from 'react';

import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

import { Button, Stack, Typography } from '@mui/material';

import { getCookies } from '@/apis/AppUser/cookie';
import { useGetMy } from '@/apis/AppUser/useGetMy';
import { usePatchMy } from '@/apis/AppUser/usePatchAppUser';
import AddPhoto from '@/components/AddPhoto';
import LabelAndInput from '@/components/LabelAndInput';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';
import { showToast } from '@/utils/showToast';
import NarrowLayout from '@/routers/NarrowLayout';

type TProfileEditForm = {
    username: string;
    password: string;
    passwordCheck: string;
    idCardFile: File | null;
};

const MyProfileEdit = () => {
    const { t } = useTranslation();
    const { openDialog } = useDialog();
    const role = getCookies('role');
    const { data: my } = useGetMy();

    const { watch, setValue, handleSubmit, register, reset } = useForm<TProfileEditForm>({
        defaultValues: {
            username: '',
            password: '',
            passwordCheck: '',
            idCardFile: null,
        },
    });

    const handleClickCancel = () => {
        openDialog({
            title: t('계정 수정 취소'),
            description: t('계정 수정을 취소하시겠습니까?'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: () => {
                    reset();
                },
            },
            secondaryAction: {
                name: t('닫기'),
                onClick: () => { },
            },
        });
    };

    const { mutateAsync: patchMy } = usePatchMy();
    const onSubmit = (formData: TProfileEditForm) => {
        if (formData.password !== formData.passwordCheck) {
            showToast.error('비밀번호가 일치하지 않습니다.');
            return;
        }
        const { passwordCheck: _, ...rest } = formData;
        openDialog({
            title: t('내 정보 수정'),
            description: t('정보를 수정하시겠습니까?'),
            variant: 'confirm',
            primaryAction: {
                name: t('확인'),
                onClick: async () => {
                    const res = await patchMy({ ...rest });
                    if (res.code === 'SUCCESS') {
                        showToast.success(t('내 정보 수정 완료'));
                    } else {
                        showToast.error(res.message);
                    }
                },
            },
            secondaryAction: {
                name: t('닫기'),
                onClick: () => { },
            },
        });
    };

    useEffect(() => {
        if (my) {
            reset({
                username: my.appUser.username,
                password: '',
                passwordCheck: '',
                idCardFile: null,
            });
        }
    }, [my, reset]);

    if (!my) return null;
    return (
        <NarrowLayout>
            <Stack sx={{ width: '100%' }} component="form" onSubmit={handleSubmit(onSubmit)}>
                <Title title={t('내 정보 수정')}>
                    <Stack
                        sx={{
                            'flexDirection': 'row',
                            'gap': '5px',
                            'alignItems': 'center',
                            '& .MuiButton-root': { height: '40px' },
                        }}
                    >
                        <Button variant="containedGrey" onClick={handleClickCancel}>
                            {t('취소')}
                        </Button>
                        <Button variant="containedBlue" type="submit">
                            {t('수정')}
                        </Button>
                    </Stack>
                </Title>
                <Stack sx={{ gap: '20px', padding: '0 32px' }}>
                    <LabelAndInput
                        sx={{ width: '100%' }}
                        fieldName="username"
                        register={register}
                        labelValue={t('이름')}
                        placeholder={t('이름')}
                    />
                    <LabelAndInput
                        sx={{ width: '100%' }}
                        fieldName="password"
                        register={register}
                        labelValue={t('새 비밀번호')}
                        placeholder={t('새 비밀번호')}
                        type="password"
                    />
                    <LabelAndInput
                        sx={{ width: '100%' }}
                        fieldName="passwordCheck"
                        register={register}
                        labelValue={t('새 비밀번호 확인')}
                        placeholder={t('새 비밀번호 확인')}
                        type="password"
                    />
                    {/* 부관리자의 경우에만 IDCard 노출 */}
                    {role.startsWith('VICE_ADMIN') && (
                        <Stack gap={'10px'}>
                            <Typography sx={{ fontSize: '14px' }}>ID Card</Typography>
                            <AddPhoto
                                fieldName="idCardFile"
                                watch={watch}
                                setValue={setValue}
                                currentUrl={my?.idCardUrl || undefined}
                            />
                        </Stack>
                    )}
                </Stack>
            </Stack>
        </NarrowLayout>

    );
};

export default MyProfileEdit;
