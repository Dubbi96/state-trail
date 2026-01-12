import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

import { Button, Stack } from '@mui/material';

import { useSignUp } from '@/apis/AppUser/useSignUp';
import { useGetArea } from '@/apis/Area/useGetArea';
import { useGetSectionList } from '@/apis/Area/useGetSectionList';
import LabelAndInput from '@/components/LabelAndInput';
import LabelAndSelect from '@/components/LabelAndSelect';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';
import NarrowLayout from '@/routers/NarrowLayout';
import { TRole } from '@/typings/User';
import { showToast } from '@/utils/showToast';

type TSignUpForm = {
    userId: string;
    username: string;
    password: string;
    passwordCheck: string;
    role: '' | TRole;
    areaId: number;
    sectionId?: number;
    idCardFile?: File;
};

const AccountRegister = () => {
    const { t } = useTranslation();
    const { openDialog } = useDialog();
    const { data: areas } = useGetArea();

    const { mutateAsync: signUp } = useSignUp();
    const { control, handleSubmit, reset, register, watch } = useForm<TSignUpForm>({
        defaultValues: {
            userId: '',
            username: '',
            password: '',
            passwordCheck: '',
            role: '',
        },
    });

    const watchedAreaId = watch('areaId');

    const { data: areaWithSections } = useGetSectionList(
        watchedAreaId ? Number(watchedAreaId) : undefined
    );

    // const handleClickCancel = () => {
    //     openDialog({
    //         title: t('계정 생성 취소'),
    //         description: t('계정 생성을 취소하시겠습니까?'),
    //         variant: 'alert',
    //         primaryAction: {
    //             name: t('확인'),
    //             onClick: () => {
    //                 reset();
    //             },
    //         },
    //         secondaryAction: {
    //             name: t('닫기'),
    //             onClick: () => {},
    //         },
    //     });
    // };

    const onSubmit = (formData: TSignUpForm) => {
        const { passwordCheck: _, sectionId, areaId, ...rest } = formData;

        const payload = {
            ...rest,
            areaId: Number(areaId),
            sectionId: sectionId ? Number(sectionId) : undefined,
        };

        openDialog({
            title: t('계정 생성'),
            description: t('계정을 생성하시겠습니까?'),
            variant: 'confirm',
            primaryAction: {
                name: t('확인'),
                onClick: async () => {
                    await signUp(payload)
                        .then((res) => {
                            if (res.code === 'SUCCESS') {
                                showToast.success(t('계정이 생성 되었습니다.'));
                                reset();
                            } else showToast.error(t('계정 생성에 실패하였습니다.'));
                        })
                        .catch(() => {
                            showToast.error(t('계정 생성에 실패하였습니다.'));
                        });
                },
            },
            secondaryAction: {
                name: t('닫기'),
                onClick: () => {},
            },
        });
    };

    return (
        <NarrowLayout>
            <Stack sx={{ width: '100%' }} component="form" onSubmit={handleSubmit(onSubmit)}>
                <Title title={t('계정 생성')}>
                    <Stack
                        sx={{
                            'flexDirection': 'row',
                            'gap': '5px',
                            '& .MuiButton-root': { height: '40px' },
                        }}
                    >
                        <Button variant="containedBlue" type="submit">
                            {t('생성')}
                        </Button>
                    </Stack>
                </Title>
                <Stack sx={{ gap: '27px', padding: '0 32px' }}>
                    <LabelAndInput
                        sx={{ width: '100%' }}
                        fieldName="username"
                        register={register}
                        labelValue={t('이름')}
                        placeholder={t('이름')}
                    />
                    <LabelAndInput
                        sx={{ width: '100%' }}
                        fieldName="userId"
                        register={register}
                        labelValue={t('아이디')}
                        placeholder={t('아이디')}
                    />
                    <LabelAndInput
                        sx={{ width: '100%' }}
                        fieldName="password"
                        register={register}
                        labelValue={t('비밀번호')}
                        placeholder={t('비밀번호')}
                        type="password"
                    />
                    <LabelAndInput
                        sx={{ width: '100%' }}
                        fieldName="passwordCheck"
                        register={register}
                        labelValue={t('비밀번호 확인')}
                        placeholder={t('비밀번호 확인')}
                        type="password"
                    />
                    <LabelAndSelect
                        labelValue={t('권한')}
                        fieldName="role"
                        control={control}
                        sx={{ width: '100%' }}
                        selectArr={[
                            { value: 'VICE_ADMIN_HEAD_OFFICER', label: t('부관리자(한국지사)') },
                            {
                                value: 'VICE_ADMIN_AGRICULTURE_MINISTRY_OFFICER',
                                label: t('부관리자(농림부)'),
                            },
                            { value: 'VILLAGE_HEAD', label: t('면장') },
                        ]}
                        placeholder={t('권한')}
                    />
                    <LabelAndSelect
                        labelValue={t('지역')}
                        fieldName="areaId"
                        sx={{ width: '100%' }}
                        control={control}
                        selectArr={
                            areas?.map((v) => {
                                return {
                                    value: v.id?.toString(),
                                    label: v.areaName,
                                };
                            }) || []
                        }
                        placeholder={t('지역')}
                    />
                    {watch('role') === 'VILLAGE_HEAD' && (
                        <LabelAndSelect
                            labelValue={t('섹션')}
                            fieldName="sectionId"
                            sx={{ width: '100%' }}
                            control={control}
                            selectArr={
                                (areaWithSections &&
                                    areaWithSections[0]?.sections?.map((v) => {
                                        return {
                                            value: v.id?.toString(),
                                            label: v.sectionName,
                                        };
                                    })) ||
                                []
                            }
                            placeholder={t('섹션')}
                            disabled={!watch('areaId')}
                        />
                    )}
                </Stack>
            </Stack>
        </NarrowLayout>
    );
};
export default AccountRegister;
