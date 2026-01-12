import React from 'react';

import { DialogContext } from '@/components/Dialog/DialogContext';

export const useDialog = () => {
    const context = React.useContext(DialogContext);

    if (!context) throw new Error('useDialog must be used within a DialogContext.');

    return context;
};
