import React from 'react';

import { FieldValues, Path, PathValue, UseFormSetValue, UseFormWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

import { Button, Stack, StackProps, Typography } from '@mui/material';

import LabelComponentsLayout from '@/components/LabelComponentsLayout';
import { palette } from '@/themes';

type LabelAndSelectFileProps<T extends FieldValues> = StackProps & {
    labelValue: string;
    fieldName: Path<T>;
    watch: UseFormWatch<T>;
    setValue: UseFormSetValue<T>;
    inputAccept?: string;
};
const LabelAndSelectFile = <T extends FieldValues>({
    labelValue,
    fieldName,
    watch,
    setValue,
    inputAccept = 'image/*',
    ...props
}: LabelAndSelectFileProps<T>) => {
    const { t } = useTranslation();
    const fileInputRef = React.useRef<HTMLInputElement | null>(null);
    const file: File | null = watch(fieldName) as File | null;

    const handleClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (file) {
            setValue(fieldName, file as PathValue<T, Path<T>>);
        }
    };
    return (
        <LabelComponentsLayout {...props} labelValue={labelValue} sx={{ ...props.sx }}>
            <Stack direction={'row'} gap="10px" sx={{ alignItems: 'center', width: '100%' }}>
                <Typography
                    sx={{
                        padding: '10px 12px',
                        height: '44px',
                        color: palette.grey[400],
                        background: palette.grey[50],
                        borderRadius: '4px',
                        fontSize: '13px',
                        flex: 1
                    }}
                >
                    {file ? file.name : t('선택된 파일 없음')}
                </Typography>
                <input
                    type="file"
                    accept={inputAccept}
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                    onChange={handleFileChange}
                />
                <Button
                    variant="containedWhite"
                    onClick={handleClick}
                    sx={{
                        height: '30px',
                        width: '84px',
                        fontSize: '14px',
                        whiteSpace: 'noWrap',
                    }}
                >
                    {t('파일 첨부')}
                </Button>
            </Stack>
        </LabelComponentsLayout>
    );
};

export default LabelAndSelectFile;
