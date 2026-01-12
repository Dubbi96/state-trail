import React, { useEffect } from 'react';

import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

import { Close } from '@mui/icons-material';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import { GoogleMap, Marker } from '@react-google-maps/api';
import { useQueryClient } from '@tanstack/react-query';

import { useGetVillageHeadDetails } from '@/apis/AppUser/useGetVillageHeadDetails';
import { CreateApprovalVillageHeadRegisterReq } from '@/apis/Approval/useCreateApprovalVillageHeadRegister';
import { useUpdateApprovalVillageHead } from '@/apis/Approval/useUpdateApprovalVillageHead';
import { useGetArea } from '@/apis/Area/useGetArea';
import { useGetMyArea } from '@/apis/Area/useGetMyArea';
import { useGetSectionList } from '@/apis/Area/useGetSectionList';
import { QUERY_KEYS } from '@/apis/QueryKeys';
import AddPhoto from '@/components/AddPhoto';
import LabelAndInput from '@/components/LabelAndInput';
import LabelAndSelect from '@/components/LabelAndSelect';
import LabelAndSelectFile from '@/components/LabelAndSelectFile';
import LabelComponentsLayout from '@/components/LabelComponentsLayout';
import { useDialog } from '@/hooks/useDialog';
import { containerStyle } from '@/pages/Locations/LocationRegister';

type EditApprovalVillageHeadRegisterForm = Omit<
    CreateApprovalVillageHeadRegisterReq,
    'sectionId'
> & { sectionId: string };

type EditVillageHeadDialogProps = {
    open: boolean;
    onClose: () => void;
    id: string;
};

const EditVillageHeadDialog = ({ open, onClose, id }: EditVillageHeadDialogProps) => {
    const { t } = useTranslation();
    const { openDialog } = useDialog();
    const { data: villageHead, isLoading } = useGetVillageHeadDetails(id);
    const { mutateAsync: updateVillageHead } = useUpdateApprovalVillageHead();
    const { data: myArea } = useGetMyArea();
    const { data: getAreaList } = useGetArea();
    const [selectArea, setSelectArea] = React.useState<string>();
    const { data: sectionList } = useGetSectionList(
        myArea?.id ?? (selectArea ? Number(selectArea) : undefined)
    );
    const queryClient = useQueryClient();
    const methods = useForm<EditApprovalVillageHeadRegisterForm>({
        defaultValues: {
            userId: villageHead?.userId,
            username: villageHead?.username,
            accountInfo: villageHead?.accountInfo,
            sectionId: JSON.stringify(villageHead?.sectionInfo.sectionId),
        },
    });
    const onSubmit = (data: EditApprovalVillageHeadRegisterForm) => {
        const submit = updateVillageHead;
        submit({ ...data, sectionId: Number(data.sectionId), appUserId: id || '' })
            .then((res) => {
                if (res?.data?.code === 'SUCCESS') {
                    queryClient.invalidateQueries({
                        queryKey: QUERY_KEYS.APP_USER.getVillageHeadList(),
                    });
                    openDialog({
                        title: t('면장 수정 요청 성공'),
                        description: t('관리자가 수정 승인 후 목록에서 확인 가능합니다.'),
                        variant: 'confirm',
                        primaryAction: {
                            name: t('확인'),
                            onClick: () => {
                                methods.reset();
                            },
                        },
                    });
                    return;
                }
                openDialog({
                    title: t('면장 수정 요청 실패'),
                    description: t('권한 확인 또는 관리자에게 문의해주세요.'),
                    variant: 'alert',
                    primaryAction: {
                        name: t('확인'),
                        onClick: () => {},
                    },
                });
            })
            .catch((err) => {
                openDialog({
                    title: t('면장 수정 요청 실패'),
                    description: `${t('에러')} : ${err}.\n 관리자에게 문의 바랍니다.`,
                    variant: 'alert',
                    primaryAction: {
                        name: t('확인'),
                        onClick: () => {
                            methods.reset();
                        },
                    },
                });
            });
    };

    const sectionInfo = sectionList?.[0].sections.find(
        (section) => String(section.id) === methods.watch('sectionId')
    );

    const handleClose = () => {
        onClose();
    };

    useEffect(() => {
        if (!villageHead) return;

        methods.reset({
            userId: villageHead.userId,
            username: villageHead.username,
            bankName: villageHead.bankName,
            accountInfo: villageHead.accountInfo,
            sectionId: String(villageHead.sectionInfo.sectionId),
        });

        setSelectArea(String(villageHead.areaInfo.areaId));
    }, [villageHead, methods]);

    if (isLoading) return <>...loading</>;
    return (
        <Dialog open={open} onClose={handleClose}>
            <DialogTitle>{t('면장 수정')}</DialogTitle>
            <IconButton
                aria-label="close"
                onClick={handleClose}
                sx={(theme) => ({
                    position: 'absolute',
                    right: 8,
                    top: 8,
                    color: theme.palette.grey[500],
                })}
            >
                <Close />
            </IconButton>
            <DialogContent>
                <form onSubmit={methods.handleSubmit(onSubmit)} id="edit-village-head-form">
                    <Stack gap={'12px'} width="100%">
                        <Stack gap={'12px'}>
                            <Typography fontSize={'16px'} fontWeight={700}>
                                {t('사진')}
                            </Typography>
                            <AddPhoto
                                fieldName={'identificationPhoto'}
                                watch={methods.watch}
                                setValue={methods.setValue}
                            />
                        </Stack>
                        <Stack direction="row" gap="12px" width="100%">
                            <LabelComponentsLayout labelValue={t('지역')} sx={{ width: '100%' }}>
                                <Select
                                    value={selectArea}
                                    onChange={(e) => setSelectArea(e.target.value)}
                                >
                                    {getAreaList?.map((area) => (
                                        <MenuItem value={String(area.id)}>{area.areaName}</MenuItem>
                                    ))}
                                </Select>
                            </LabelComponentsLayout>
                            <LabelAndSelect
                                sx={{ width: '100%' }}
                                labelValue={t('관리 섹션')}
                                fieldName="sectionId"
                                control={methods.control}
                                placeholder={t('관리 섹션')}
                                selectArr={
                                    sectionList?.[0].sections?.map((section) => {
                                        return {
                                            value: String(section.id),
                                            label: section.sectionName,
                                        };
                                    }) || []
                                }
                            />
                        </Stack>

                        <Stack>
                            <GoogleMap
                                mapContainerStyle={containerStyle}
                                center={{
                                    lat: sectionInfo?.latitude || 0,
                                    lng: sectionInfo?.longitude || 0,
                                }}
                                zoom={13}
                            >
                                <Marker
                                    position={{
                                        lat: sectionInfo?.latitude || 0,
                                        lng: sectionInfo?.longitude || 0,
                                    }}
                                />
                            </GoogleMap>
                        </Stack>
                        <LabelAndInput
                            labelValue={t('아이디')}
                            fieldName="userId"
                            register={methods.register}
                            placeholder={t('아이디를 입력해주세요.')}
                            disabled={true}
                        />
                        <LabelAndInput
                            labelValue={t('이름')}
                            fieldName="username"
                            register={methods.register}
                            placeholder={t('이름을 입력해주세요.')}
                        />
                        <LabelComponentsLayout labelValue={t('계좌 정보')} sx={{ width: '100%' }}>
                            <Stack direction={'row'} gap="12px">
                                <TextField
                                    {...methods.register('bankName')}
                                    placeholder={t('은행명')}
                                />
                                <TextField
                                    sx={{ width: '500px' }}
                                    {...methods.register('accountInfo')}
                                    placeholder={t('계좌 번호')}
                                />
                            </Stack>
                        </LabelComponentsLayout>

                        <LabelAndSelectFile
                            labelValue={t('계약서')}
                            fieldName={'contractFile'}
                            watch={methods.watch}
                            setValue={methods.setValue}
                            inputAccept="image/*,application/pdf"
                        />
                        <LabelAndSelectFile
                            labelValue={t('통장 사본')}
                            fieldName={'bankbookPhoto'}
                            watch={methods.watch}
                            setValue={methods.setValue}
                            inputAccept="image/*,application/pdf"
                        />
                    </Stack>
                </form>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} sx={{ flex: 1 }} variant="containedGrey">
                    취소
                </Button>
                <Button
                    type="submit"
                    form="create-village-head-form"
                    variant="containedBlue"
                    sx={{ flex: 1 }}
                >
                    {t('확인')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default EditVillageHeadDialog;
