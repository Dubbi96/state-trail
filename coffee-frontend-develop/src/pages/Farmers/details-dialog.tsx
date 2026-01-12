import { useGetFarmerDetail } from "@/apis/Farmer/useGetFarmerDetail";
import LabelComponentsLayout from "@/components/LabelComponentsLayout";
import { palette } from "@/themes";
import { Close } from "@mui/icons-material";
import { Dialog, DialogContent, DialogTitle, IconButton, Stack, TextField, Typography } from "@mui/material";
import { useTranslation } from "react-i18next";

type FarmerDetailsDialogProps = {
    open: boolean;
    onClose: () => void;
    farmerId: number;
}

export const FarmerDetailsDialog = ({ open, onClose, farmerId }: FarmerDetailsDialogProps) => {
    const { t } = useTranslation();
    const { data: farmer, isLoading: farmerDetailLoading } = useGetFarmerDetail(String(farmerId));

    const handleClose = () => {
        onClose();
    };

    if (farmerDetailLoading || !farmer) return <>...loading</>
    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{t('농부 상세 정보')}</DialogTitle>
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
                    <LabelComponentsLayout labelValue={t('면장')}>
                        <TextField value={farmer?.villageHeadName} slotProps={{ input: { readOnly: true } }} />
                    </LabelComponentsLayout>

                    <LabelComponentsLayout labelValue={t('이름')}>
                        <TextField value={farmer?.farmerName} slotProps={{ input: { readOnly: true } }} />
                    </LabelComponentsLayout>
                    <LabelComponentsLayout labelValue={t('사진')}>
                        <Stack
                            sx={{
                                backgroundColor: palette.grey[50],
                                width: '120px',
                                height: '160px',
                                justifyContent: 'center',
                                alignItems: 'center',
                            }}
                        >
                            {farmer?.identificationPhotoUrl ? <img
                                src={farmer?.identificationPhotoUrl}
                                alt="current image"
                                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            /> : <Typography color={palette.grey[400]}>No Image</Typography>}
                        </Stack>
                    </LabelComponentsLayout>
                </Stack>
            </DialogContent>
        </Dialog>
    );
};