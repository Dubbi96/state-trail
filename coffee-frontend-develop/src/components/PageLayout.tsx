import React, { PropsWithChildren } from 'react';

import { Stack, StackProps } from '@mui/material';

const PageLayout: React.FC<StackProps & PropsWithChildren> = ({ children, ...props }) => {
    return (
        <Stack {...props} sx={{ ...props.sx, padding: '0 32px', flex: 1, minHeight: 0 }}>
            {children}
        </Stack>
    );
};

export default PageLayout;
