import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { Close } from '@mui/icons-material';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Stack,
} from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';

import { useGetViceAdminDetails } from '@/apis/AppUser/useGetViceAdminDetails';
import { useUpdateViceAdminDetails } from '@/apis/AppUser/useUpdateViceAdminDetails';
import { useGetArea } from '@/apis/Area/useGetArea';
import { QUERY_KEYS } from '@/apis/QueryKeys';
import AddPhoto from '@/components/AddPhoto';
import LabelAndInput from '@/components/LabelAndInput';
import LabelAndSelect from '@/components/LabelAndSelect';
import LabelComponentsLayout from '@/components/LabelComponentsLayout';
import { useDialog } from '@/hooks/useDialog';

type TViceAdminDetailsInput = {
    viceAdminId: number;
    username: string;
    userId: string;
    areaId: string;
    idCardFile: File | null;
};

type EditViceAdminDialogProps = {
    open: boolean;
    onClose: () => void;
    viceAdminId: number;
};

export const EditViceAdminDialog = ({ open, onClose, viceAdminId }: EditViceAdminDialogProps) => {
    const { t } = useTranslation();

    const handleClose = () => {
        onClose();
    };

    const queryClient = useQueryClient();
    const { openDialog } = useDialog();
    const { data: viceAdminDetail } = useGetViceAdminDetails(String(viceAdminId));
    const { mutateAsync: updateViceAdmin } = useUpdateViceAdminDetails();
    const { data: getAreaList } = useGetArea();
    const navigate = useNavigate();
    const methods = useForm<TViceAdminDetailsInput>({
        defaultValues: {
            viceAdminId: Number(viceAdminId),
            username: viceAdminDetail?.username || '',
            userId: viceAdminDetail?.userId || '',
            idCardFile: null,
            areaId: String(viceAdminDetail?.areaInfo.areaId || ''),
        },
    });

    const onSubmitEdit = (data: TViceAdminDetailsInput) => {
        updateViceAdmin({ ...data, areaId: Number(data.areaId) })
            .then((res) => {
                if (res.data.code === 'SUCCESS') {
                    queryClient.invalidateQueries({
                        queryKey: QUERY_KEYS.APP_USER.getViceAdminDetail(
                            JSON.stringify(viceAdminId)
                        ),
                    });
                    openDialog({
                        title: t('부관리자 수정 완료'),
                        variant: 'confirm',
                        primaryAction: {
                            name: t('확인'),
                            onClick: () => {
                                navigate(`/vice-admins/${viceAdminId}`);
                            },
                        },
                    });
                    return;
                }
                openDialog({
                    title: t('부관리자 수정 실패'),
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
                    title: t('부관리자 수정 실패'),
                    description: `${t('에러')} : ${err}.\n 관리자에게 문의 바랍니다.`,
                    variant: 'alert',
                    primaryAction: {
                        name: '확인',
                        onClick: () => {
                            methods.reset();
                        },
                    },
                });
            });
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{t('부관리자 정보 수정')}</DialogTitle>
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
                <Stack gap={'12px'}>
                    <LabelComponentsLayout labelValue={t('ID Card')}>
                        <AddPhoto
                            fieldName="idCardFile"
                            currentUrl={viceAdminDetail?.idCardUrl || ''}
                            watch={methods.watch}
                            setValue={methods.setValue}
                        />
                    </LabelComponentsLayout>
                    <LabelAndInput
                        register={methods.register}
                        labelValue={t('이름')}
                        fieldName="username"
                        placeholder={t('이름을 입력해주세요.')}
                    />
                    <LabelAndInput
                        register={methods.register}
                        labelValue={t('아이디')}
                        fieldName="userId"
                        disabled={true}
                        placeholder={t('아이디를 입력해주세요.')}
                    />
                    <LabelAndSelect
                        control={methods.control}
                        labelValue={t('관리 지역')}
                        fieldName="areaId"
                        selectArr={
                            getAreaList?.map((area) => {
                                return { value: String(area.id), label: area.areaName };
                            }) || []
                        }
                        placeholder={t('관리 지역을 선택해주세요.')}
                    />
                    {/* <Stack>
                        <GoogleMap
                            mapContainerStyle={containerStyle}
                            center={{
                                lat: areaInfo?.latitude || 0,
                                lng: areaInfo?.longitude || 0,
                            }}
                            zoom={13}
                        >
                            <Marker
                                position={{
                                    lat: areaInfo?.latitude || 0,
                                    lng: areaInfo?.longitude || 0,
                                }}
                            />
                        </GoogleMap>
                    </Stack> */}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} sx={{ flex: 1 }} variant="containedGrey">
                    취소
                </Button>
                <Button
                    onClick={methods.handleSubmit(onSubmitEdit)}
                    form="edit-vice-admin-form"
                    variant="containedBlue"
                    sx={{ flex: 1 }}
                >
                    {t('확인')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
