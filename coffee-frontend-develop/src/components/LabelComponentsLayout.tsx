import React from 'react';

import { Stack, StackProps, Typography } from '@mui/material';
import { palette } from '@/themes';

type LabelComponentsLayoutProps = StackProps & {
    labelValue: string;
    children?: React.ReactNode;
};
const LabelComponentsLayout: React.FC<LabelComponentsLayoutProps> = ({
    children,
    labelValue,
    ...props
}) => {
    return (
        <Stack {...props} sx={{ gap: '12px', ...props.sx }}>
            <Typography sx={{ fontSize: '16px', fontWeight: 600, color: palette.grey[700] }}>{labelValue}</Typography>
            {children}
        </Stack>
    );
};

export default LabelComponentsLayout;
